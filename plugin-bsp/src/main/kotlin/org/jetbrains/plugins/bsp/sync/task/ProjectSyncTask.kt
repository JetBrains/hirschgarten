package org.jetbrains.plugins.bsp.sync.task

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
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
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.bsp.action.saveAllFiles
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.performance.bspTracer
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.sync.ProjectPostSyncHook
import org.jetbrains.plugins.bsp.sync.ProjectPreSyncHook
import org.jetbrains.plugins.bsp.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.plugins.bsp.sync.additionalProjectPostSyncHooks
import org.jetbrains.plugins.bsp.sync.additionalProjectPreSyncHooks
import org.jetbrains.plugins.bsp.sync.additionalProjectSyncHooks
import org.jetbrains.plugins.bsp.sync.defaultProjectPostSyncHooks
import org.jetbrains.plugins.bsp.sync.defaultProjectPreSyncHooks
import org.jetbrains.plugins.bsp.sync.defaultProjectSyncHooks
import org.jetbrains.plugins.bsp.sync.projectStructure.AllProjectStructuresProvider
import org.jetbrains.plugins.bsp.sync.scope.ProjectSyncScope
import org.jetbrains.plugins.bsp.sync.status.BspSyncStatusService
import org.jetbrains.plugins.bsp.sync.status.SyncAlreadyInProgressException
import org.jetbrains.plugins.bsp.ui.console.ids.PROJECT_SYNC_TASK_ID
import org.jetbrains.plugins.bsp.ui.console.syncConsole
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

private val log = logger<ProjectSyncTask>()

class ProjectSyncTask(private val project: Project) {
  suspend fun sync(syncScope: ProjectSyncScope, buildProject: Boolean) {
    if (project.isTrusted()) {
      coroutineScope {
        bspTracer.spanBuilder("bsp.sync.project.ms").useWithScope {
          var syncAlreadyInProgress = false
          try {
            log.debug("Starting sync project task")
            project.syncConsole.startTask(
              taskId = PROJECT_SYNC_TASK_ID,
              title = BspPluginBundle.message("console.task.sync.title"),
              message = BspPluginBundle.message("console.task.sync.in.progress"),
              cancelAction = {
                BspSyncStatusService.getInstance(project).cancel()
                coroutineContext.cancel()
              },
              redoAction = { sync(syncScope, buildProject) },
            )

            preSync()
            doSync(syncScope, buildProject)

            project.syncConsole.finishTask(PROJECT_SYNC_TASK_ID, BspPluginBundle.message("console.task.sync.success"))
          } catch (_: CancellationException) {
            project.syncConsole.finishTask(
              PROJECT_SYNC_TASK_ID,
              BspPluginBundle.message("console.task.sync.cancelled"),
              SkippedResultImpl(),
            )
          } catch (_: SyncAlreadyInProgressException) {
            syncAlreadyInProgress = true
          } catch (e: Exception) {
            log.debug("BSP sync failed")
            project.syncConsole.finishTask(
              PROJECT_SYNC_TASK_ID,
              BspPluginBundle.message("console.task.sync.failed"),
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
  }

  private suspend fun preSync() {
    log.debug("Running pre sync tasks")
    saveAllFiles()
    BspSyncStatusService.getInstance(project).startSync()
  }

  private suspend fun doSync(syncScope: ProjectSyncScope, buildProject: Boolean) {
    val syncActivityName = BspPluginBundle.message("console.task.sync.activity.name", project.assets.presentableName)
    withSuspendScanningAndIndexing(syncActivityName) {
      withBackgroundProgress(project, "Syncing project...", true) {
        reportSequentialProgress {
          executePreSyncHooks(it)
          executeSyncHooks(it, syncScope, buildProject)
          executePostSyncHooks(it)
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
    val environment =
      ProjectPreSyncHook.ProjectPreSyncHookEnvironment(
        project = project,
        taskId = PROJECT_SYNC_TASK_ID,
        progressReporter = progressReporter,
      )

    project.defaultProjectPreSyncHooks.forEach {
      it.onPreSync(environment)
    }
    project.additionalProjectPreSyncHooks.forEach {
      it.onPreSync(environment)
    }
  }

  private suspend fun executeSyncHooks(
    progressReporter: SequentialProgressReporter,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
  ) {
    val diff = AllProjectStructuresProvider(project).newDiff()
    project.connection.runWithServer { server, capabilities ->
      bspTracer.spanBuilder("collect.project.details.ms").use {
        val baseTargetInfos = BaseProjectSync(project).execute(syncScope, buildProject, server, capabilities, PROJECT_SYNC_TASK_ID)
        val environment =
          ProjectSyncHookEnvironment(
            project = project,
            server = server,
            capabilities = capabilities,
            diff = diff,
            taskId = PROJECT_SYNC_TASK_ID,
            progressReporter = progressReporter,
            baseTargetInfos = baseTargetInfos,
            syncScope = syncScope,
          )

        project.defaultProjectSyncHooks.forEach {
          it.onSync(environment)
        }
        project.additionalProjectSyncHooks.forEach {
          it.onSync(environment)
        }
      }
    }

    diff.applyAll(syncScope, PROJECT_SYNC_TASK_ID)
  }

  private suspend fun executePostSyncHooks(progressReporter: SequentialProgressReporter) {
    val environment =
      ProjectPostSyncHook.ProjectPostSyncHookEnvironment(
        project = project,
        taskId = PROJECT_SYNC_TASK_ID,
        progressReporter = progressReporter,
      )

    project.defaultProjectPostSyncHooks.forEach {
      it.onPostSync(environment)
    }
    project.additionalProjectPostSyncHooks.forEach {
      it.onPostSync(environment)
    }
  }

  private suspend fun postSync() {
    BspSyncStatusService.getInstance(project).finishSync()
    withContext(Dispatchers.EDT) {
      ProjectView.getInstance(project).refresh()
    }
  }
}

fun <Result> CoroutineScope.asyncQueryIf(
  check: Boolean,
  queryName: String,
  doQuery: () -> CompletableFuture<Result>,
): Deferred<Result?> = async { queryIf(check, queryName, doQuery) }

suspend fun <Result> queryIf(
  check: Boolean,
  queryName: String,
  doQuery: () -> CompletableFuture<Result>,
): Result? = if (check) query(queryName, doQuery) else null

fun <Result> CoroutineScope.asyncQuery(queryName: String, doQuery: () -> CompletableFuture<Result>): Deferred<Result> =
  async { query(queryName, doQuery) }

suspend fun <Result> query(queryName: String, doQuery: () -> CompletableFuture<Result>): Result =
  try {
    withContext(Dispatchers.IO) { doQuery().await() }
  } catch (e: Exception) {
    when (e) {
      is CancellationException -> fileLogger().info("Query $queryName is cancelled")
      else -> fileLogger().warn("Query $queryName failed", e)
    }
    throw e
  }
