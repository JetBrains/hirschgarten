package org.jetbrains.bazel.sync.task

import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.UnindexedFilesScannerExecutor
import com.intellij.openapi.vfs.findDirectory
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.ui.treeStructure.ProjectViewUpdateCause
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.run.task.BazelBuildTaskListener
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectPostSyncHooks
import org.jetbrains.bazel.sync.projectPreSyncHooks
import org.jetbrains.bazel.sync.projectStructure.ProjectModelApplicatonTask
import org.jetbrains.bazel.sync.projectSyncHooks
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.status.SyncAlreadyInProgressException
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.workspace.fileEvents.FileEventJobManager
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import java.util.concurrent.CancellationException
import kotlin.random.Random

private val log = logger<ProjectSyncTask>()

@ApiStatus.Internal
class ProjectSyncTask(private val project: Project) {
  suspend fun sync(syncScope: ProjectSyncScope, buildProject: Boolean) {
    if (TrustedProjects.isProjectTrusted(project)) {
      bspTracer.spanBuilder("bsp.sync.project.ms").setAttribute("project.name", project.name).useWithScope {
        val syncConsole = project.syncConsole
        val taskId = TaskGroupId("sync-${project.name}-${Random.nextBytes(8).toHexString()}").task("project-sync")

        try {
          project.serviceAsync<SyncStatusService>().startSync()
        }
        catch (_: SyncAlreadyInProgressException) {
          return@useWithScope
        }

        FileEventJobManager.getInstance(project).syncTaskGroupId = taskId.taskGroupId

        try {
          log.debug("Starting sync project task")

          try {
            val taskListener = BazelBuildTaskListener(syncConsole)
            BazelTaskEventsService.getInstance(project).saveListener(taskId.taskGroupId, taskListener)

            val syncJob = BazelCoroutineService.getInstance(project).startAsync(lazy = true) {
              preSync()
              doSync(taskId, syncScope, buildProject)
            }

            syncConsole.startTask(
              taskId = taskId,
              title = BazelPluginBundle.message("console.task.sync.title"),
              message = BazelPluginBundle.message("console.task.sync.in.progress"),
              cancelAction = {
                SyncStatusService.getInstance(project).cancel()
                syncJob.cancel()
              },
              redoAction = { sync(syncScope, buildProject) },
            )

            when (val syncResult = syncJob.await()) {
              SyncResultStatus.FAILURE -> {
                syncConsole.finishTask(
                  taskId,
                  BazelPluginBundle.message("console.task.sync.fatalfailure"),
                  FailureResultImpl(),
                )
              }
              SyncResultStatus.PARTIAL_SUCCESS -> {
                syncConsole.addDiagnosticMessage(
                  taskId,
                  null, -1, -1,
                  BazelPluginBundle.message("console.task.sync.partialsuccess"),
                  MessageEvent.Kind.WARNING
                )
                syncConsole.finishTask(
                  taskId,
                  BazelPluginBundle.message("console.task.sync.partialsuccess"),
                  SuccessResultImpl(true),
                )
              }
              SyncResultStatus.SUCCESS -> {
                syncConsole.finishTask(
                  taskId,
                  BazelPluginBundle.message("console.task.sync.success")
                )
              }
            }
          }
          finally {
            BazelTaskEventsService.getInstance(project).removeListener(taskId.taskGroupId)
          }
        }
        catch (e: CancellationException) {
          syncConsole.finishTask(
            taskId,
            BazelPluginBundle.message("console.task.sync.cancelled"),
            SkippedResultImpl(),
          )
          throw e
        }
        catch (e: Exception) {
          log.error("Error syncing project", e)
          syncConsole.finishTask(
            taskId,
            BazelPluginBundle.message("console.task.sync.failed"),
            FailureResultImpl(e),
          )
        }
        finally {
          SyncStatusService.getInstance(project).finishSync()
          withContext(Dispatchers.EDT) {
            ProjectView.getInstance(project).refresh(ProjectViewUpdateCause.PLUGIN_BAZEL)
          }
        }
      }
    }
  }

  private suspend fun preSync() {
    log.debug("Running pre sync tasks")
    saveAllFiles()
    clearSyntheticTargets()
  }

  private suspend fun clearSyntheticTargets() {
    writeAction {
      project.rootDir.findDirectory(Constants.DOT_BAZELBSP_DIR_NAME)
        ?.findDirectory("synthetic_targets")
        ?.children
        ?.forEach { it.delete(this) }
    }
  }

  private suspend fun doSync(taskId: TaskId, syncScope: ProjectSyncScope, buildProject: Boolean): SyncResultStatus {
    val syncActivityName =
      BazelPluginBundle.message(
        "console.task.sync.activity.name",
        BazelPluginConstants.BAZEL_DISPLAY_NAME,
      )
    val saveAndSyncHandler = serviceAsync<SaveAndSyncHandler>()
    var syncResult = SyncResultStatus.FAILURE
    UnindexedFilesScannerExecutor.getInstance(project).suspendScanningAndIndexingThenExecute(syncActivityName) {
      saveAndSyncHandler.disableAutoSave().use {
        withBackgroundProgress(project, BazelPluginBundle.message("background.progress.syncing.project"), true) {
          reportSequentialProgress { progressReporter ->
            executePreSyncHooks(progressReporter, taskId)
            var shouldUpdateProjectModel = false
            BazelServerService.getInstance(project).connection.runWithServer { server ->
              server.outFileHardLinks.onBeforeSync()
              try {
                server.bazelInfo.release.deprecated()?.let { deprecated ->
                  project.syncConsole.addDiagnosticMessage(
                    taskId, null, -1, -1,
                    "$deprecated Sync might give incomplete results.",
                    MessageEvent.Kind.WARNING,
                  )
                }

                val storage = MutableEntityStorage.create()
                val deferredApplyActions = mutableListOf<suspend () -> Unit>()
                syncResult = executeSyncHooks(
                  progressReporter = progressReporter,
                  syncScope = syncScope,
                  buildProject = buildProject,
                  storage = storage,
                  taskId = taskId,
                  server = server,
                  deferredApplyActions = deferredApplyActions,
                )
                shouldUpdateProjectModel = syncResult != SyncResultStatus.FAILURE
                if (shouldUpdateProjectModel) {
                  updateProjectModel(
                    progressReporter = progressReporter,
                    syncScope = syncScope,
                    storage = storage,
                    taskId = taskId,
                    deferredApplyActions = deferredApplyActions,
                  )
                }
              } finally {
                server.outFileHardLinks.onAfterSync(shouldUpdateProjectModel)
              }
            }

            executePostSyncHooks(progressReporter = progressReporter, taskId = taskId, projectModelUpdated = shouldUpdateProjectModel)
          }
        }
      }
    }
    saveAndSyncHandler.scheduleProjectSave(project = project)
    return syncResult
  }

  private suspend fun executePreSyncHooks(progressReporter: SequentialProgressReporter, taskId: TaskId) {
    project.syncConsole.withSubtask(
      reporter = progressReporter,
      subtaskId = taskId.subTask("pre-sync-hooks"),
      text = BazelPluginBundle.message("console.task.execute.pre.sync.hooks"),
    ) { subtaskId ->
      val environment =
        ProjectPreSyncHook.ProjectPreSyncHookEnvironment(
          project = project,
          taskId = subtaskId,
          progressReporter = progressReporter,
        )

      project.projectPreSyncHooks.forEachSubtask(subtaskId) {
        it.onPreSync(environment)
      }
    }
  }

  private suspend fun executeSyncHooks(
    progressReporter: SequentialProgressReporter,
    taskId: TaskId,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    storage: MutableEntityStorage,
    server: BazelServerFacade,
    deferredApplyActions: MutableList<suspend () -> Unit>,
  ): SyncResultStatus {
    val resolver = BazelWorkspaceResolveService.getInstance(project)
    val syncStatus =
      bspTracer.spanBuilder("collect.project.details.ms").use {
        // if this bazel build fails, we still want the sync hooks to be executed
        val bazelProject =
          project.syncConsole.withSubtask(
            subtaskId = taskId.subTask("base-project-sync-subtask-id"),
            message = BazelPluginBundle.message("console.task.base.sync"),
          ) { subtaskId ->
            // force full re-sync
            resolver.invalidateCachedState()
            resolver.getOrFetchSyncedProject(build = buildProject, taskId = subtaskId).also {
              it.targets.values.filter { it.tagsList.any { it.equals(Constants.NO_IDE) }}.let {
                if (!it.isEmpty()) {
                  project.syncConsole.addDiagnosticMessage(
                    subtaskId, null, -1, -1,
                    "Included ${it.size} ${Constants.NO_IDE} targets as dependencies: ${it.joinToString(",", limit = 5) { it.label().toString() }}",
                    MessageEvent.Kind.WARNING
                  )
                }
              }
            }
          }
        if (bazelProject.hasError && bazelProject.targets.isEmpty()) return@use SyncResultStatus.FAILURE
        project.syncConsole.withSubtask(
          reporter = progressReporter,
          subtaskId = taskId.subTask("sync-hooks"),
          text = BazelPluginBundle.message("console.task.execute.sync.hooks"),
        ) { subtaskId ->
          val environment =
            ProjectSyncHookEnvironment(
              project = project,
              server = server,
              resolver = resolver,
              diff = storage,
              taskId = subtaskId,
              progressReporter = progressReporter,
              buildTargets = bazelProject.targets,
              syncScope = syncScope,
              workspace = resolver.getOrFetchResolvedWorkspace(scope = syncScope, taskId = subtaskId),
              deferredApplyActions = deferredApplyActions,
            )

          project.projectSyncHooks.forEachSubtask(subtaskId) {
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

    return syncStatus
  }

  private suspend fun updateProjectModel(
    progressReporter: SequentialProgressReporter,
    syncScope: ProjectSyncScope,
    storage: MutableEntityStorage,
    taskId: TaskId,
    deferredApplyActions: MutableList<suspend () -> Unit>,
  ) {
    project.syncConsole.withSubtask(
      reporter = progressReporter,
      subtaskId = taskId.subTask("apply-changes"),
      text = BazelPluginBundle.message("console.task.apply.changes"),
    ) { subtaskId ->
      val applicator = ProjectModelApplicatonTask(
        project = project,
        scope = syncScope,
        taskId = subtaskId,
        postActions = deferredApplyActions,
      )
      applicator.apply(storage)
    }
  }

  private suspend fun executePostSyncHooks(progressReporter: SequentialProgressReporter, taskId: TaskId, projectModelUpdated: Boolean) {
    project.syncConsole.withSubtask(
      reporter = progressReporter,
      subtaskId = taskId.subTask("post-sync-hooks"),
      text = BazelPluginBundle.message("console.task.execute.post.sync.hooks"),
    ) { subtaskId ->
      val environment =
        ProjectPostSyncHook.ProjectPostSyncHookEnvironment(
          project = project,
          taskId = subtaskId,
          progressReporter = progressReporter,
          projectModelUpdated = projectModelUpdated,
        )

      project.projectPostSyncHooks.forEachSubtask(subtaskId) {
        it.onPostSync(environment)
      }
    }
  }

  private suspend fun <T> List<T>.forEachSubtask(taskId: TaskId, action: suspend (T) -> Unit) {
    forEach { item ->
      try {
        action(item)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Throwable) {
        if (project.syncConsole.registerException(taskId, e)) {
          project.syncConsole.addDiagnosticMessage(
            taskId,
            null, -1, -1,
            e.message ?: "Unknown error",
            MessageEvent.Kind.ERROR,
          )
        }
        log.error(e)
      }
    }
  }

  private enum class SyncResultStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILURE,
  }
}
