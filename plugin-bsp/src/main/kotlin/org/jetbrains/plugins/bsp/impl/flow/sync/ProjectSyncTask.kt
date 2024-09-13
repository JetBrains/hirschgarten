package org.jetbrains.plugins.bsp.impl.flow.sync

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.ide.impl.isTrusted
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.fileLogger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
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
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.bsp.action.saveAllFiles
import org.jetbrains.plugins.bsp.building.syncConsole
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.plugins.bsp.impl.projectAware.BspSyncStatusService
import org.jetbrains.plugins.bsp.impl.projectAware.SyncAlreadyInProgressException
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import org.jetbrains.plugins.bsp.performance.bspTracer
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresProvider
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture

private const val PROJECT_SYNC_TASK_ID = "project-sync"

private val log = logger<ProjectSyncTask>()

class ProjectSyncTask(private val project: Project) {
  suspend fun sync(buildProject: Boolean) {
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
            )

            preSync()
            doSync(buildProject)

            project.syncConsole.finishTask(PROJECT_SYNC_TASK_ID, BspPluginBundle.message("console.task.sync.success"))
          } catch (e: CancellationException) {
            project.syncConsole.finishTask(
              PROJECT_SYNC_TASK_ID,
              BspPluginBundle.message("console.task.sync.cancelled"),
              FailureResultImpl(),
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
    BspSyncStatusService.getInstance(project).startSync()
    saveAllFiles()
  }

  private suspend fun doSync(buildProject: Boolean) {
    withBackgroundProgress(project, "Syncing project...", true) {
      reportSequentialProgress {
        executePreSyncHooks(it)
        executeSyncHooks(it, buildProject)
        executePostSyncHooks(it)
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

  private suspend fun executeSyncHooks(progressReporter: SequentialProgressReporter, buildProject: Boolean) {
    val diff = AllProjectStructuresProvider(project).newDiff()
    project.connection.runWithServer { server, capabilities ->
      bspTracer.spanBuilder("collect.project.details.ms").use {
        val baseTargetInfos = BaseProjectSync(project).execute(buildProject, server, capabilities, PROJECT_SYNC_TASK_ID)
        val environment =
          ProjectSyncHookEnvironment(
            project = project,
            server = server,
            capabilities = capabilities,
            diff = diff,
            taskId = PROJECT_SYNC_TASK_ID,
            progressReporter = progressReporter,
            baseTargetInfos = baseTargetInfos,
          )

        project.defaultProjectSyncHooks.forEach {
          it.onSync(environment)
        }
        project.additionalProjectSyncHooks.forEach {
          it.onSync(environment)
        }
      }
    }

    diff.applyAll(PROJECT_SYNC_TASK_ID)
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
