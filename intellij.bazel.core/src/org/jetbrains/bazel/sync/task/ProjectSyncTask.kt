package org.jetbrains.bazel.sync.task

import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.IncompleteDependenciesService
import com.intellij.openapi.project.IncompleteDependenciesService.IncompleteDependenciesAccessToken
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.UnindexedFilesScannerExecutor
import com.intellij.openapi.vfs.findDirectory
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.fus.BazelSyncCollector
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.run.task.BazelBuildTaskListener
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectPostSyncHooks
import org.jetbrains.bazel.sync.projectPreSyncHooks
import org.jetbrains.bazel.sync.projectStructure.ProjectModelApplicationTask
import org.jetbrains.bazel.sync.projectSyncHooks
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.SyncAlreadyInProgressException
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterHelper
import org.jetbrains.bazel.sync.workspace.mapper.BazelWorkspaceResolver
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.allTargets
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.workspace.fileEvents.FileEventJobManager
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.allSources
import java.util.concurrent.CancellationException
import kotlin.random.Random

private val log = logger<ProjectSyncTask>()

// TODO: some parts of this logic should be moved to `backend` module
@ApiStatus.Internal
class ProjectSyncTask(private val project: Project) {
  suspend fun fullSync(buildProject: Boolean) {
    sync(SecondPhaseSync, buildProject)
  }

  suspend fun phasedSync(runSecondPhase: Boolean, buildProject: Boolean) {
    var incompleteState: IncompleteDependenciesAccessToken? = null
    try {
      val firstPhaseResult = sync(FirstPhaseSync, false)
      if (firstPhaseResult.completionResult == ProjectSyncCompletionResult.CANCELLED ||
          firstPhaseResult.completionResult == ProjectSyncCompletionResult.SKIPPED) {
        return
      }
      incompleteState =
        edtWriteAction {
          project.service<IncompleteDependenciesService>().enterIncompleteState(this)
        }

      if (runSecondPhase) {
        sync(SecondPhaseSync, buildProject)
      }
    }
    finally {
      if (incompleteState != null && runSecondPhase) {
        edtWriteAction { incompleteState.finish() }
      }
    }
  }

  suspend fun partialSync(targets: List<Label>, buildProject: Boolean) {
    sync(PartialProjectSync(targetsToSync = targets), buildProject = buildProject)
  }

  private suspend fun sync(syncScope: ProjectSyncScope, buildProject: Boolean): ProjectSyncResult {
    if (!TrustedProjects.isProjectTrusted(project)) return ProjectSyncResult(ProjectSyncCompletionResult.SKIPPED)

    return bspTracer.spanBuilder("bsp.sync.project.ms").setAttribute("project.name", project.name).useWithScope {
      runSyncTask(syncScope, buildProject)
    }
  }

  @Suppress("IncorrectCancellationExceptionHandling")
  private suspend fun runSyncTask(syncScope: ProjectSyncScope, buildProject: Boolean): ProjectSyncResult {
    val syncConsole = project.syncConsole
    val taskId = TaskGroupId("sync-${project.name}-${Random.nextBytes(8).toHexString()}").task("project-sync")

    try {
      project.serviceAsync<SyncStatusService>().startSync()
    }
    catch (_: SyncAlreadyInProgressException) {
      BazelSyncCollector.logSyncSkipped(project, syncScope, buildProject)
      return ProjectSyncResult(ProjectSyncCompletionResult.SKIPPED)
    }

    FileEventJobManager.getInstance(project).syncTaskGroupId = taskId.taskGroupId

    try {
      log.debug("Starting sync project task")

      try {
        val taskListener = BazelBuildTaskListener(syncConsole)
        BazelTaskEventsService.getInstance(project).saveListener(taskId.taskGroupId, taskListener)

        val syncJob = BazelCoroutineService.getInstance(project).startAsync(lazy = true) {
          BazelSyncCollector.logSync(project, syncScope, buildProject) {
            doSync(taskId, syncScope, buildProject)
          }
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

        val syncResult = syncJob.await()
        when (syncResult.completionResult) {
          ProjectSyncCompletionResult.FAILURE -> {
            val failureCause = syncResult.failureCause
            failureCause?.let { log.error("Error syncing project", it) }
            // distinguish a thrown error mid-sync from a clean run that resolved no targets
            val message =
              if (failureCause != null) BazelPluginBundle.message("console.task.sync.failed")
              else BazelPluginBundle.message("console.task.sync.fatalfailure")
            syncConsole.finishTask(
              taskId,
              message,
              failureCause?.let(::FailureResultImpl) ?: FailureResultImpl(),
            )
          }

          ProjectSyncCompletionResult.PARTIAL_SUCCESS -> {
            syncConsole.addDiagnosticMessage(
              taskId,
              null, -1, -1,
              message = BazelPluginBundle.message("console.task.sync.partialsuccess"),
              description = null,
              MessageEvent.Kind.WARNING,
            )
            syncConsole.finishTask(
              taskId,
              BazelPluginBundle.message("console.task.sync.partialsuccess"),
              SuccessResultImpl(true),
            )
          }

          ProjectSyncCompletionResult.SUCCESS -> {
            syncConsole.finishTask(
              taskId,
              BazelPluginBundle.message("console.task.sync.success"),
            )
          }

          ProjectSyncCompletionResult.CANCELLED -> {
            syncConsole.finishTask(
              taskId,
              BazelPluginBundle.message("console.task.sync.cancelled"),
              SkippedResultImpl(),
            )
          }

          ProjectSyncCompletionResult.SKIPPED -> Unit
        }
        return syncResult
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
      return ProjectSyncResult(ProjectSyncCompletionResult.CANCELLED, failureCause = e)
    }
    catch (e: Exception) {
      log.error("Error syncing project", e)
      syncConsole.finishTask(
        taskId,
        BazelPluginBundle.message("console.task.sync.failed"),
        FailureResultImpl(e),
      )
      return ProjectSyncResult(ProjectSyncCompletionResult.FAILURE, failureCause = e)
    }
    finally {
      SyncStatusService.getInstance(project).finishSync()
    }
  }

  private suspend fun preSync() {
    log.debug("Running pre sync tasks")
    saveAllFiles()
    clearSyntheticTargets()
  }

  private suspend fun clearSyntheticTargets() {
    edtWriteAction {
      project.rootDir.findDirectory(Constants.DOT_BAZELBSP_DIR_NAME)
        ?.findDirectory("synthetic_targets")
        ?.children
        ?.forEach { it.delete(this) }
    }
  }

  @Suppress("IncorrectCancellationExceptionHandling")
  private suspend fun doSync(
    taskId: TaskId,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
  ): ProjectSyncResult {
    val syncActivityName =
      BazelPluginBundle.message(
        "console.task.sync.activity.name",
        BazelPluginConstants.BAZEL_DISPLAY_NAME,
      )
    val saveAndSyncHandler = serviceAsync<SaveAndSyncHandler>()
    val phaseDurations = mutableListOf<ProjectSyncPhaseDuration>()
    var syncResult = ProjectSyncResult(ProjectSyncCompletionResult.FAILURE)
    return try {
      preSync()
      UnindexedFilesScannerExecutor.getInstance(project).suspendScanningAndIndexingThenExecute(syncActivityName) {
        saveAndSyncHandler.disableAutoSave().use {
          withBackgroundProgress(project, BazelPluginBundle.message("background.progress.syncing.project"), true) {
            reportSequentialProgress { progressReporter ->
              syncResult = executeSyncPipeline(
                progressReporter = progressReporter,
                taskId = taskId,
                syncScope = syncScope,
                buildProject = buildProject,
                phaseDurations = phaseDurations,
              )
            }
          }
        }
      }
      saveAndSyncHandler.scheduleProjectSave(project = project)
      syncResult.copy(phaseDurations = phaseDurations.toList())
    }
    catch (e: CancellationException) {
      syncResult.copy(
        completionResult = ProjectSyncCompletionResult.CANCELLED,
        phaseDurations = phaseDurations.toList(),
        failureCause = e,
      )
    }
    catch (e: Exception) {
      syncResult.copy(
        completionResult = ProjectSyncCompletionResult.FAILURE,
        phaseDurations = phaseDurations.toList(),
        failureCause = e,
      )
    }
  }

  private suspend fun executeSyncPipeline(
    progressReporter: SequentialProgressReporter,
    taskId: TaskId,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    phaseDurations: MutableList<ProjectSyncPhaseDuration>,
  ): ProjectSyncResult {
    var shouldUpdateProjectModel = false
    try {
      executePreSyncHooks(progressReporter, taskId)
      return BazelServerService.getInstance(project).connection.runWithServer(taskId) { server ->
        server.withOutFileHardLinksSync(projectModelUpdated = { shouldUpdateProjectModel }) {
          server.bazelInfo.release.deprecated()?.let { deprecated ->
            project.syncConsole.addDiagnosticMessage(
              taskId = taskId,
              message = "$deprecated Sync might give incomplete results.",
              severity = MessageEvent.Kind.WARNING,
            )
          }

          if (!server.bazelInfo.isConfigurationSupportEnabled) {
            project.syncConsole.addDiagnosticMessage(
              taskId = taskId,
              message = BazelPluginBundle.message("console.task.sync.configurations.unsupported"),
              severity = MessageEvent.Kind.WARNING,
            )
          }

          val storage = MutableEntityStorage.create()
          val deferredApplyActions = mutableListOf<suspend () -> Unit>()
          val syncResult = phaseDurations.trackSyncPhase(ProjectSyncPhase.COLLECT_PROJECT_DETAILS) {
            executeSyncHooks(
              progressReporter = progressReporter,
              syncScope = syncScope,
              buildProject = buildProject,
              storage = storage,
              taskId = taskId,
              server = server,
              deferredApplyActions = deferredApplyActions,
              importerHelper = WorkspaceImporterHelper(
                project = project,
                taskConsole = project.syncConsole,
                progressReporter = progressReporter,
                taskId = taskId,
                builder = storage,
              ),
            )
          }
          shouldUpdateProjectModel = syncResult.completionResult != ProjectSyncCompletionResult.FAILURE
          if (shouldUpdateProjectModel) {
            phaseDurations.trackSyncPhase(ProjectSyncPhase.APPLY_PROJECT_MODEL) {
              updateProjectModel(
                progressReporter = progressReporter,
                syncScope = syncScope,
                storage = storage,
                taskId = taskId,
                deferredApplyActions = deferredApplyActions,
              )
            }
          }
          syncResult
        }
      }
    }
    finally {
      executePostSyncHooks(
        progressReporter = progressReporter,
        taskId = taskId,
        projectModelUpdated = shouldUpdateProjectModel,
      )
    }
  }

  private suspend fun <T> BazelServerFacade.withOutFileHardLinksSync(
    projectModelUpdated: () -> Boolean,
    action: suspend () -> T,
  ): T {
    outFileHardLinks.onBeforeSync()
    try {
      return action()
    }
    finally {
      outFileHardLinks.onAfterSync(projectModelUpdated())
    }
  }

  private suspend fun executePreSyncHooks(
    progressReporter: SequentialProgressReporter,
    taskId: TaskId,
  ) {
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

  // remember from first phase to second phase for proper sharding
  private var allKnownTargets: List<Label>? = null

  private suspend fun executeSyncHooks(
    progressReporter: SequentialProgressReporter,
    taskId: TaskId,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    storage: MutableEntityStorage,
    server: BazelServerFacade,
    importerHelper: WorkspaceImporterHelper,
    deferredApplyActions: MutableList<suspend () -> Unit>,
  ): ProjectSyncResult {
    return bspTracer.spanBuilder("collect.project.details.ms").use {
      // if this bazel build fails, we still want the sync hooks to be executed
      val resolvedWorkspace =
        project.syncConsole.withSubtask(
          subtaskId = taskId.subTask("base-project-sync-subtask-id"),
          message = BazelPluginBundle.message("console.task.base.sync"),
        ) { subtaskId ->
          BazelWorkspaceResolver.fetchWorkspace(
            project,
            scope = syncScope,
            build = buildProject,
            allKnownTargets = allKnownTargets,
            taskId = subtaskId,
          )
        }
      val statistics = resolvedWorkspace.targets.syncStatistics()
      if (resolvedWorkspace.hasError && resolvedWorkspace.targets.isEmpty())
        return@use ProjectSyncResult(ProjectSyncCompletionResult.FAILURE, statistics = statistics)
      if (syncScope == FirstPhaseSync) {
        allKnownTargets = resolvedWorkspace.targets.map { it.id }
      }

      project.syncConsole.withSubtask(
        reporter = progressReporter,
        subtaskId = taskId.subTask("sync-hooks"),
        text = BazelPluginBundle.message("console.task.execute.sync.hooks"),
      ) { subtaskId ->
        val workspaceSnapshot = WorkspaceSnapshotBuilder.build(
          project = project,
          workspaceContext = server.workspaceContext,
          repoMapping = resolvedWorkspace.repoMapping,
          resolved = resolvedWorkspace,
        )
        // importers first
        importerHelper.invoke(progressReporter, workspaceSnapshot)
        val environment =
          ProjectSyncHookEnvironment(
            project = project,
            server = server,
            diff = storage,
            taskId = subtaskId,
            progressReporter = progressReporter,
            syncScope = syncScope,
            workspace = resolvedWorkspace,
            deferredApplyActions = deferredApplyActions,
          )
        saveTargetStorage(workspaceSnapshot)
        // then sync hooks
        project.projectSyncHooks.forEachSubtask(subtaskId) {
          it.onSync(environment)
        }
        deferredApplyActions += { importerHelper.invokeLate(progressReporter, workspaceSnapshot) }
        if (resolvedWorkspace.hasError) {
          ProjectSyncResult(ProjectSyncCompletionResult.PARTIAL_SUCCESS, statistics = statistics)
        }
        else {
          ProjectSyncResult(ProjectSyncCompletionResult.SUCCESS, statistics = statistics)
        }
      }
    }
  }

  // TODO: remove this after proper `WorkspaceSnapshot` persistance
  private fun saveTargetStorage(snapshot: WorkspaceSnapshot) {
    project.targetUtils.saveTargets(
      targets = snapshot.allTargets
        .map { it.rawBuildTarget }
        .toList(),
      fileToTarget = snapshot.allTargets
        .flatMap { target -> target.rawBuildTarget.allSources.map { source -> source to target } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second.targetKey.label })
        .toMap(),
    )
    project.targetUtils.notifyTargetListUpdated()
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
      val applicator = ProjectModelApplicationTask(
        project = project,
        scope = syncScope,
        taskId = subtaskId,
        postActions = deferredApplyActions,
      )
      applicator.apply(storage)
    }
  }

  private suspend fun executePostSyncHooks(
    progressReporter: SequentialProgressReporter,
    taskId: TaskId,
    projectModelUpdated: Boolean,
  ) {
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
      }
      catch (e: CancellationException) {
        throw e
      }
      catch (e: Throwable) {
        if (project.syncConsole.registerException(taskId, e)) {
          project.syncConsole.addDiagnosticMessage(
            taskId,
            null, -1, -1,
            message = e.message ?: "Unknown error",
            description = null,
            MessageEvent.Kind.ERROR,
          )
        }
        log.error(e)
      }
    }
  }
}
