package org.jetbrains.bazel.sync.task

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.UnindexedFilesScannerExecutor
import com.intellij.openapi.vfs.findDirectory
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.ui.treeStructure.ProjectViewUpdateCause
import com.intellij.util.containers.forEachLoggingErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.run.task.BazelBuildTaskListener
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.server.connection.connection
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
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import java.util.UUID
import java.util.concurrent.CancellationException
import kotlin.random.Random

private val log = logger<ProjectSyncTask>()

class ProjectSyncTask(private val project: Project) {
  suspend fun sync(syncScope: ProjectSyncScope, buildProject: Boolean) {
    if (TrustedProjects.isProjectTrusted(project)) {
      bspTracer.spanBuilder("bsp.sync.project.ms").setAttribute("project.name", project.name).useWithScope {
        var syncAlreadyInProgress = false
        val syncConsole = project.syncConsole
        val taskId = TaskGroupId("sync-${project.name}-${Random.nextBytes(8).toHexString()}").task("project-sync")
        try {
          log.debug("Starting sync project task")
          val taskListener = BazelBuildTaskListener(syncConsole)
          BazelTaskEventsService.getInstance(project).saveListener(taskId.taskGroupId, taskListener)

          try {
            syncConsole.startTask(
              taskId = taskId,
              title = BazelPluginBundle.message("console.task.sync.title"),
              message = BazelPluginBundle.message("console.task.sync.in.progress"),
              cancelAction = {
                SyncStatusService.getInstance(project).cancel()
                coroutineContext.cancel()
              },
              redoAction = { sync(syncScope, buildProject) },
            )

            preSync()
            doSync(taskId, syncScope, buildProject)
          }
          finally {
            BazelTaskEventsService.getInstance(project).removeListener(taskId.taskGroupId)
          }

          syncConsole.finishTask(taskId, BazelPluginBundle.message("console.task.sync.success"))
        }
        catch (e: CancellationException) {
          syncConsole.finishTask(
            taskId,
            BazelPluginBundle.message("console.task.sync.cancelled"),
            SkippedResultImpl(),
          )
          throw e
        }
        catch (_: SyncAlreadyInProgressException) {
          syncAlreadyInProgress = true
        }
        catch (_: SyncPartialFailureException) {
          syncConsole.addWarnMessage(
            taskId,
            BazelPluginBundle.message("console.task.sync.partialsuccess"),
          )
          syncConsole.finishTask(
            taskId,
            BazelPluginBundle.message("console.task.sync.partialsuccess"),
            SuccessResultImpl(true),
          )
        }
        catch (_: SyncFatalFailureException) {
          syncConsole.finishTask(
            taskId,
            BazelPluginBundle.message("console.task.sync.fatalfailure"),
            FailureResultImpl(),
          )
        }
        catch (e: Exception) {
          syncConsole.finishTask(
            taskId,
            BazelPluginBundle.message("console.task.sync.failed"),
            FailureResultImpl(e),
          )
        }
        finally {
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
    clearSyntheticTargets()
    project.serviceAsync<ProjectViewService>().forceReparseCurrentProjectViewFiles()
    project.serviceAsync<SyncStatusService>().startSync()
  }

  private suspend fun clearSyntheticTargets() {
    writeAction {
      project.rootDir.findDirectory(".bazelbsp")
        ?.findDirectory("synthetic_targets")
        ?.children
        ?.forEach { it.delete(this) }
    }
  }

  private suspend fun doSync(taskId: TaskId, syncScope: ProjectSyncScope, buildProject: Boolean) {
    val syncActivityName =
      BazelPluginBundle.message(
        "console.task.sync.activity.name",
        BazelPluginConstants.BAZEL_DISPLAY_NAME,
      )
    val saveAndSyncHandler = serviceAsync<SaveAndSyncHandler>()
    UnindexedFilesScannerExecutor.getInstance(project).suspendScanningAndIndexingThenExecute(syncActivityName) {
      saveAndSyncHandler.disableAutoSave().use {
        withBackgroundProgress(project, BazelPluginBundle.message("background.progress.syncing.project"), true) {
          reportSequentialProgress {
            executePreSyncHooks(it, taskId)
            BazelServerService.getInstance(project).connection.runWithServer {
              it.bazelInfo.release.deprecated()?.let {
                project.syncConsole.addWarnMessage(taskId, it + " Sync might give incomplete results.")
              }
            }
            val syncResult = executeSyncHooks(it, taskId, syncScope, buildProject)
            executePostSyncHooks(it, taskId)
            when (syncResult) {
              SyncResultStatus.FAILURE -> throw SyncFatalFailureException()
              SyncResultStatus.PARTIAL_SUCCESS -> throw SyncPartialFailureException()
              else -> Unit
            }
          }
        }
      }
    }
    saveAndSyncHandler.scheduleProjectSave(project = project)
  }

  private suspend fun executePreSyncHooks(progressReporter: SequentialProgressReporter, taskId: TaskId) {
    project.withSubtask(
      reporter = progressReporter,
      subtaskId = taskId.subTask("pre-sync-hooks"),
      text = BazelPluginBundle.message("console.task.execute.pre.sync.hooks"),
    ) {
      val environment =
        ProjectPreSyncHook.ProjectPreSyncHookEnvironment(
          project = project,
          taskId = it,
          progressReporter = progressReporter,
        )

      project.projectPreSyncHooks.forEachLoggingErrors(log) {
        it.onPreSync(environment)
      }
    }
  }

  private suspend fun executeSyncHooks(
    progressReporter: SequentialProgressReporter,
    taskId: TaskId,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
  ): SyncResultStatus {
    val diff = AllProjectStructuresProvider(project).newDiff()
    val resolver = BazelWorkspaceResolveService.getInstance(project)
    val syncStatus =
      project.connection.runWithServer { server ->
        bspTracer.spanBuilder("collect.project.details.ms").use {
          // if this bazel build fails, we still want the sync hooks to be executed
          val bazelProject =
            project.syncConsole.withSubtask(
              subtaskId = taskId.subTask("base-project-sync-subtask-id"),
              message = BazelPluginBundle.message("console.task.base.sync"),
            ) {
              // force full re-sync
              resolver.invalidateCachedState()
              resolver.getOrFetchSyncedProject(build = buildProject, taskId = it)
            }
          if (bazelProject.hasError && bazelProject.targets.isEmpty()) return@use SyncResultStatus.FAILURE
          project.withSubtask(
            reporter = progressReporter,
            subtaskId = taskId.subTask("sync-hooks"),
            text = BazelPluginBundle.message("console.task.execute.sync.hooks"),
          ) {
            val environment =
              ProjectSyncHookEnvironment(
                project = project,
                server = server,
                resolver = resolver,
                diff = diff,
                taskId = it,
                progressReporter = progressReporter,
                buildTargets = bazelProject.targets,
                syncScope = syncScope,
              )

            project.projectSyncHooks.forEachLoggingErrors(log) {
              it.onSync(environment)
            }
          }

          if (bazelProject.hasError) {
            SyncResultStatus.PARTIAL_SUCCESS
          }
          else {
            SyncResultStatus.SUCCESS
          }
        }
      }

    if (syncStatus != SyncResultStatus.FAILURE) {
      project.withSubtask(
        reporter = progressReporter,
        subtaskId = taskId.subTask("apply-changes"),
        text = BazelPluginBundle.message("console.task.apply.changes"),
      ) { diff.applyAll(syncScope, it) }
    }
    return syncStatus
  }

  private suspend fun executePostSyncHooks(progressReporter: SequentialProgressReporter, taskId: TaskId) {
    project.withSubtask(
      reporter = progressReporter,
      subtaskId = taskId.subTask("post-sync-hooks"),
      text = BazelPluginBundle.message("console.task.execute.post.sync.hooks"),
    ) {
      val environment =
        ProjectPostSyncHook.ProjectPostSyncHookEnvironment(
          project = project,
          taskId = it,
          progressReporter = progressReporter,
        )

      project.projectPostSyncHooks.forEachLoggingErrors(log) {
        it.onPostSync(environment)
      }
    }
  }

  private suspend fun postSync() {
    SyncStatusService.getInstance(project).finishSync()
    withContext(Dispatchers.EDT) {
      ProjectView.getInstance(project).refresh(ProjectViewUpdateCause.PLUGIN_BAZEL)
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
  }
  catch (e: Exception) {
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
