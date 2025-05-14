package org.jetbrains.bazel.sync.task

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.ide.impl.isTrusted
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.UnindexedFilesScannerExecutor
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.projectview.parser.ProjectViewParser
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectPostSyncHooks
import org.jetbrains.bazel.sync.projectPreSyncHooks
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresProvider
import org.jetbrains.bazel.sync.projectSyncHooks
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.status.SyncAlreadyInProgressException
import org.jetbrains.bazel.sync.status.SyncFatalFailureException
import org.jetbrains.bazel.sync.status.SyncPartialFailureException
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.ui.console.ids.BASE_PROJECT_SYNC_SUBTASK_ID
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import java.nio.file.NoSuchFileException
import java.util.concurrent.CancellationException

private val log = logger<ProjectSyncTask>()

class ProjectSyncTask(private val project: Project) {
  suspend fun sync(syncScope: ProjectSyncScope, buildProject: Boolean) {
    if (project.isTrusted()) {
      bspTracer.spanBuilder("bsp.sync.project.ms").useWithScope {
        var syncAlreadyInProgress = false
        try {
          log.debug("Starting sync project task")
          project.syncConsole.startTask(
            taskId = PROJECT_SYNC_TASK_ID,
            title = BazelPluginBundle.message("console.task.sync.title"),
            message = BazelPluginBundle.message("console.task.sync.in.progress"),
            cancelAction = {
              SyncStatusService.getInstance(project).cancel()
              coroutineContext.cancel()
            },
            redoAction = { sync(syncScope, buildProject) },
          )

          preSync()
          doSync(syncScope, buildProject)

          project.syncConsole.finishTask(PROJECT_SYNC_TASK_ID, BazelPluginBundle.message("console.task.sync.success"))
        } catch (e: CancellationException) {
          project.syncConsole.finishTask(
            PROJECT_SYNC_TASK_ID,
            BazelPluginBundle.message("console.task.sync.cancelled"),
            SkippedResultImpl(),
          )
          throw e
        } catch (_: SyncAlreadyInProgressException) {
          syncAlreadyInProgress = true
        } catch (_: SyncPartialFailureException) {
          project.syncConsole.addWarnMessage(
            PROJECT_SYNC_TASK_ID,
            BazelPluginBundle.message("console.task.sync.partialsuccess"),
          )
          project.syncConsole.finishTask(
            PROJECT_SYNC_TASK_ID,
            BazelPluginBundle.message("console.task.sync.partialsuccess"),
            SuccessResultImpl(true),
          )
        } catch (_: SyncFatalFailureException) {
          project.syncConsole.finishTask(
            PROJECT_SYNC_TASK_ID,
            BazelPluginBundle.message("console.task.sync.fatalfailure"),
            FailureResultImpl(),
          )
        } catch (e: ProjectViewParser.ImportNotFound) {
          val projectViewFile = project.bazelProjectSettings.projectViewPath.toString()
          project.syncConsole.finishTask(
            PROJECT_SYNC_TASK_ID,
            BazelPluginBundle.message("console.task.sync.failed"),
            FailureResultImpl(BazelPluginBundle.message("console.task.sync.import.fail", e.file, projectViewFile)),
          )
        } catch (e: Exception) {
          project.syncConsole.finishTask(
            PROJECT_SYNC_TASK_ID,
            BazelPluginBundle.message("console.task.sync.failed"),
            FailureResultImpl(e),
          )
        } finally {
          if (!syncAlreadyInProgress) {
            postSync()
          }
        }
      }
    }
  }

  private suspend fun preSync() {
    log.debug("Running pre sync tasks")
    saveAllFiles()
    SyncStatusService.getInstance(project).startSync()
  }

  private suspend fun doSync(syncScope: ProjectSyncScope, buildProject: Boolean) {
    val syncActivityName =
      BazelPluginBundle.message(
        "console.task.sync.activity.name",
        BazelPluginConstants.BAZEL_DISPLAY_NAME,
      )
    withSuspendScanningAndIndexing(syncActivityName) {
      withBackgroundProgress(project, "Syncing project...", true) {
        reportSequentialProgress {
          executePreSyncHooks(it)
          val syncResult = executeSyncHooks(it, syncScope, buildProject)
          executePostSyncHooks(it)
          when (syncResult) {
            SyncResultStatus.FAILURE -> throw SyncFatalFailureException()
            SyncResultStatus.PARTIAL_SUCCESS -> throw SyncPartialFailureException()
            else -> Unit
          }
        }
      }
    }
  }

  private suspend fun withSuspendScanningAndIndexing(activityName: String, activity: suspend () -> Unit) =
    coroutineScope {
      // Use Dispatchers.IO to wait for the blocking call
      withContext(Dispatchers.IO) {
        UnindexedFilesScannerExecutor.getInstance(project).suspendScanningAndIndexingThenRun(activityName) {
          runBlocking(coroutineContext) {
            activity()
          }
        }
      }
    }

  private suspend fun executePreSyncHooks(progressReporter: SequentialProgressReporter) {
    project.withSubtask(
      reporter = progressReporter,
      taskId = PROJECT_SYNC_TASK_ID,
      text = BazelPluginBundle.message("console.task.execute.pre.sync.hooks"),
    ) {
      val environment =
        ProjectPreSyncHook.ProjectPreSyncHookEnvironment(
          project = project,
          taskId = it,
          progressReporter = progressReporter,
        )

      project.projectPreSyncHooks.forEach {
        it.onPreSync(environment)
      }
    }
  }

  private suspend fun executeSyncHooks(
    progressReporter: SequentialProgressReporter,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
  ): SyncResultStatus {
    val diff = AllProjectStructuresProvider(project).newDiff()
    val syncStatus =
      project.connection.runWithServer { server ->
        bspTracer.spanBuilder("collect.project.details.ms").use {
          // if this bazel build fails, we still want the sync hooks to be executed
          val bazelProject =
            project.syncConsole.withSubtask(
              taskId = PROJECT_SYNC_TASK_ID,
              subtaskId = BASE_PROJECT_SYNC_SUBTASK_ID,
              message = BazelPluginBundle.message("console.task.base.sync"),
            ) { server.runSync(buildProject, PROJECT_SYNC_TASK_ID) }
          if (bazelProject.hasError && bazelProject.targets.isEmpty()) return@use SyncResultStatus.FAILURE
          project.withSubtask(
            reporter = progressReporter,
            taskId = PROJECT_SYNC_TASK_ID,
            text = BazelPluginBundle.message("console.task.execute.sync.hooks"),
          ) {
            val environment =
              ProjectSyncHookEnvironment(
                project = project,
                server = server,
                diff = diff,
                taskId = it,
                progressReporter = progressReporter,
                buildTargets = bazelProject.targets,
                syncScope = syncScope,
              )

            project.projectSyncHooks.forEach {
              it.onSync(environment)
            }
          }

          if (bazelProject.hasError) {
            SyncResultStatus.PARTIAL_SUCCESS
          } else {
            SyncResultStatus.SUCCESS
          }
        }
      }

    if (syncStatus != SyncResultStatus.FAILURE) {
      project.withSubtask(
        reporter = progressReporter,
        taskId = PROJECT_SYNC_TASK_ID,
        text = BazelPluginBundle.message("console.task.apply.changes"),
      ) { diff.applyAll(syncScope, it) }
    }
    return syncStatus
  }

  private suspend fun executePostSyncHooks(progressReporter: SequentialProgressReporter) {
    project.withSubtask(
      reporter = progressReporter,
      taskId = PROJECT_SYNC_TASK_ID,
      text = BazelPluginBundle.message("console.task.execute.post.sync.hooks"),
    ) {
      val environment =
        ProjectPostSyncHook.ProjectPostSyncHookEnvironment(
          project = project,
          taskId = it,
          progressReporter = progressReporter,
        )

      project.projectPostSyncHooks.forEach {
        it.onPostSync(environment)
      }
    }
  }

  private suspend fun postSync() {
    SyncStatusService.getInstance(project).finishSync()
    withContext(Dispatchers.EDT) {
      ProjectView.getInstance(project).refresh()
    }
  }
}

fun <Result> CoroutineScope.asyncQueryIf(
  check: Boolean,
  queryName: String,
  doQuery: suspend () -> Result,
): Deferred<Result?> = async { queryIf(check, queryName, doQuery) }

suspend fun <Result> queryIf(
  check: Boolean,
  queryName: String,
  doQuery: suspend () -> Result,
): Result? = if (check) query(queryName, doQuery) else null

fun <Result> CoroutineScope.asyncQuery(queryName: String, doQuery: suspend () -> Result): Deferred<Result> =
  async { query(queryName, doQuery) }

suspend fun <Result> query(queryName: String, doQuery: suspend () -> Result): Result =
  try {
    withContext(Dispatchers.IO) { doQuery() }
  } catch (e: Exception) {
    when (e) {
      is CancellationException -> fileLogger().info("Query $queryName is cancelled")
      else -> fileLogger().warn("Query $queryName failed", e)
    }
    throw e
  }

enum class SyncResultStatus {
  SUCCESS,
  PARTIAL_SUCCESS,
  FAILURE,
}
