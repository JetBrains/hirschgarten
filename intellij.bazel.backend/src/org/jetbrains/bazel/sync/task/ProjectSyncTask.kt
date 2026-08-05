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
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.fus.BazelSyncCollector
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.unresolvedRequiredImports
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
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.SyncWorkspaceUpdate
import org.jetbrains.bazel.sync.SyncWorkspaceUpdater
import org.jetbrains.bazel.sync.projectStructure.ProjectModelApplicationTask
import org.jetbrains.bazel.sync.projectSyncHooks
import org.jetbrains.bazel.sync.status.SyncAlreadyInProgressException
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.sync.workspace.importer.WorkspaceImporterHelper
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.id
import java.util.concurrent.CancellationException
import kotlin.random.Random

private val log = logger<ProjectSyncTask>()

internal class ProjectSyncTask(
  private val project: Project,
  private val scope: ProjectSyncScope,
  private val onSyncTaskStarted: (TaskId) -> Unit = {},
) {
  suspend fun sync() {
    when (scope) {
      is ProjectSyncScope.Full ->
        if (scope.phased) {
          phasedSync(buildProject = scope.build)
        }
        else {
          syncPhase(SyncPhase.SECOND, buildProject = scope.build)
        }

      is ProjectSyncScope.Targets -> throw UnsupportedOperationException("not supported yet")
      is ProjectSyncScope.Files -> throw UnsupportedOperationException("not supported yet")
    }
  }

  private suspend fun phasedSync(buildProject: Boolean) {
    var incompleteState: IncompleteDependenciesAccessToken? = null
    try {
      val firstPhaseResult = syncPhase(SyncPhase.FIRST, false)
      if (firstPhaseResult.completionResult == ProjectSyncCompletionResult.CANCELLED ||
          firstPhaseResult.completionResult == ProjectSyncCompletionResult.SKIPPED) {
        return
      }
      incompleteState =
        edtWriteAction {
          project.service<IncompleteDependenciesService>().enterIncompleteState(this)
        }

      syncPhase(SyncPhase.SECOND, buildProject)
    }
    finally {
      if (incompleteState != null) {
        edtWriteAction { incompleteState.finish() }
      }
    }
  }

  private suspend fun syncPhase(phase: SyncPhase, buildProject: Boolean): ProjectSyncResult {
    if (!TrustedProjects.isProjectTrusted(project)) return ProjectSyncResult(ProjectSyncCompletionResult.SKIPPED)

    return bspTracer.spanBuilder("bsp.sync.project.ms").setAttribute("project.name", project.name).useWithScope {
      runSyncTask(phase, buildProject)
    }
  }

  @Suppress("IncorrectCancellationExceptionHandling")
  private suspend fun runSyncTask(phase: SyncPhase, buildProject: Boolean): ProjectSyncResult {
    val syncConsole = project.syncConsole
    val taskId = TaskGroupId("sync-${project.name}-${Random.nextBytes(8).toHexString()}").task("project-sync")

    try {
      project.serviceAsync<SyncStatusService>().startSync()
    }
    catch (_: SyncAlreadyInProgressException) {
      BazelSyncCollector.logSyncSkipped(project, scope, phase, buildProject)
      return ProjectSyncResult(ProjectSyncCompletionResult.SKIPPED)
    }

    onSyncTaskStarted(taskId)

    try {
      log.debug("Starting sync project task")

      try {
        val taskListener = BazelBuildTaskListener(syncConsole)
        BazelTaskEventsService.getInstance(project).saveListener(taskId.taskGroupId, taskListener)

        val syncJob = BazelCoroutineService.getInstance(project).startAsync(lazy = true) {
          BazelSyncCollector.logSync(project, scope, phase, buildProject) {
            doSync(taskId, phase, buildProject)
          }
        }

        syncConsole.startTask(
          taskId = taskId,
          title = BazelBackendBundle.message("console.task.sync.title"),
          message = BazelBackendBundle.message("console.task.sync.in.progress"),
          cancelAction = {
            SyncStatusService.getInstance(project).cancel()
            syncJob.cancel()
          },
          redoAction = { syncPhase(phase, buildProject) },
        )

        val syncResult = syncJob.await()
        when (syncResult.completionResult) {
          ProjectSyncCompletionResult.FAILURE -> {
            val failureCause = syncResult.failureCause
            failureCause?.let { log.error("Error syncing project", it) }
            // distinguish a thrown error mid-sync from a clean run that resolved no targets
            val message =
              if (failureCause != null) BazelBackendBundle.message("console.task.sync.failed")
              else BazelBackendBundle.message("console.task.sync.fatalfailure")
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
              message = BazelBackendBundle.message("console.task.sync.partialsuccess"),
              description = null,
              MessageEvent.Kind.WARNING,
            )
            syncConsole.finishTask(
              taskId,
              BazelBackendBundle.message("console.task.sync.partialsuccess"),
              SuccessResultImpl(true),
            )
          }

          ProjectSyncCompletionResult.SUCCESS -> {
            syncConsole.finishTask(
              taskId,
              BazelBackendBundle.message("console.task.sync.success"),
            )
          }

          ProjectSyncCompletionResult.CANCELLED -> {
            syncConsole.finishTask(
              taskId,
              BazelBackendBundle.message("console.task.sync.cancelled"),
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
        BazelBackendBundle.message("console.task.sync.cancelled"),
        SkippedResultImpl(),
      )
      return ProjectSyncResult(ProjectSyncCompletionResult.CANCELLED, failureCause = e)
    }
    catch (e: Exception) {
      log.error("Error syncing project", e)
      syncConsole.finishTask(
        taskId,
        BazelBackendBundle.message("console.task.sync.failed"),
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
    phase: SyncPhase,
    buildProject: Boolean,
  ): ProjectSyncResult {
    val syncActivityName =
      BazelBackendBundle.message(
        "console.task.sync.activity.name",
        Constants.BAZEL_DISPLAY_NAME,
      )
    val saveAndSyncHandler = serviceAsync<SaveAndSyncHandler>()
    val phaseDurations = mutableListOf<ProjectSyncPhaseDuration>()
    var syncResult = ProjectSyncResult(ProjectSyncCompletionResult.FAILURE)
    return try {
      preSync()
      UnindexedFilesScannerExecutor.getInstance(project).suspendScanningAndIndexingThenExecute(syncActivityName) {
        saveAndSyncHandler.disableAutoSave().use {
          withBackgroundProgress(project, BazelBackendBundle.message("background.progress.syncing.project"), true) {
            reportSequentialProgress { progressReporter ->
              syncResult = executeSyncPipeline(
                progressReporter = progressReporter,
                taskId = taskId,
                phase = phase,
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
    phase: SyncPhase,
    buildProject: Boolean,
    phaseDurations: MutableList<ProjectSyncPhaseDuration>,
  ): ProjectSyncResult {
    var shouldUpdateProjectModel = false
    try {
      executePreSyncHooks(progressReporter, taskId)

      // A required `import` in the project view (e.g. `import local.bazelproject`) whose file is
      // missing leaves the project view effectively empty. Without stopping here the sync still
      // contacts the Bazel server and tries to resolve configurations/targets from that empty view,
      // which fails slowly with misleading secondary errors instead of the real cause. Stop now;
      // the offending import was already reported as an error by ReparseProjectViewFilePreSyncHook.
      if (project.serviceAsync<ProjectViewService>().projectView.unresolvedRequiredImports().isNotEmpty()) {
        return ProjectSyncResult(ProjectSyncCompletionResult.FAILURE)
      }

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
              message = BazelBackendBundle.message("console.task.sync.configurations.unsupported"),
              severity = MessageEvent.Kind.WARNING,
            )
          }

          val storage = MutableEntityStorage.create()
          val deferredApplyActions = mutableListOf<suspend () -> Unit>()
          val collectResult = phaseDurations.trackSyncPhase(ProjectSyncPhase.COLLECT_PROJECT_DETAILS) {
            executeSyncHooks(
              progressReporter = progressReporter,
              phase = phase,
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
          val syncResult = collectResult.syncResult
          shouldUpdateProjectModel = syncResult.completionResult != ProjectSyncCompletionResult.FAILURE
          if (shouldUpdateProjectModel) {
            phaseDurations.trackSyncPhase(ProjectSyncPhase.APPLY_PROJECT_MODEL) {
              updateProjectModel(
                progressReporter = progressReporter,
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
      text = BazelBackendBundle.message("console.task.execute.pre.sync.hooks"),
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

  private data class CollectProjectResult(
    val syncResult: ProjectSyncResult,
    val scope: ProjectSyncScope,
  )

  private suspend fun executeSyncHooks(
    progressReporter: SequentialProgressReporter,
    taskId: TaskId,
    phase: SyncPhase,
    buildProject: Boolean,
    storage: MutableEntityStorage,
    server: BazelServerFacade,
    importerHelper: WorkspaceImporterHelper,
    deferredApplyActions: MutableList<suspend () -> Unit>,
  ): CollectProjectResult {
    return bspTracer.spanBuilder("collect.project.details.ms").use {
      // if this bazel build fails, we still want the sync hooks to be executed
      val (_, syncWorkspace) =
        project.syncConsole.withSubtask(
          subtaskId = taskId.subTask("base-project-sync-subtask-id"),
          message = if (buildProject)
            BazelBackendBundle.message("console.task.base.build.sync")
          else
            BazelBackendBundle.message("console.task.base.sync"),
        ) { subtaskId ->
          val context = SyncWorkspaceContext(
            phase = phase,
            buildProject = buildProject,
            allKnownTargets = allKnownTargets,
            server = server,
            taskId = subtaskId,
          )
          // the only snapshot update of the pipeline: the provider infers the effective scope, resolves
          // and derives the new snapshot, all against the very same base snapshot
          project.service<WorkspaceSnapshotService>()
            .update { previous -> SyncWorkspaceUpdater(project).update(scope, previous, context).let { it.snapshot to it } }
        }

      // a fatal resolve republished the previous snapshot, so nothing was synced and it must not be counted
      if (syncWorkspace.status == SyncWorkspaceStatus.FATAL) {
        return@use CollectProjectResult(
          syncResult = ProjectSyncResult(ProjectSyncCompletionResult.FAILURE, statistics = emptyList<BuildTarget>().syncStatistics()),
          scope = syncWorkspace.scope,
        )
      }

      val syncedTargets = syncWorkspace.snapshot.targets.allTargets().toList()
      val statistics = syncedTargets.syncStatistics()
      if (phase == SyncPhase.FIRST) {
        allKnownTargets = syncedTargets.map { it.id }
      }

      val syncResult = project.syncConsole.withSubtask(
        reporter = progressReporter,
        subtaskId = taskId.subTask("sync-hooks"),
        text = BazelBackendBundle.message("console.task.execute.sync.hooks"),
      ) { subtaskId ->
        val workspaceSnapshot = syncWorkspace.snapshot
        // importers first
        importerHelper.invoke(progressReporter, workspaceSnapshot)
        val environment =
          ProjectSyncHookEnvironment(
            project = project,
            server = server,
            diff = storage,
            taskId = subtaskId,
            progressReporter = progressReporter,
            syncScope = syncWorkspace.scope,
            snapshot = workspaceSnapshot,
            deferredApplyActions = deferredApplyActions,
          )
        // then sync hooks
        project.projectSyncHooks.forEachSubtask(subtaskId) {
          it.onSync(environment)
        }
        deferredApplyActions += { importerHelper.invokeLate(progressReporter, workspaceSnapshot) }
        if (syncWorkspace.status == SyncWorkspaceStatus.PARTIAL) {
          ProjectSyncResult(ProjectSyncCompletionResult.PARTIAL_SUCCESS, statistics = statistics)
        }
        else {
          ProjectSyncResult(ProjectSyncCompletionResult.SUCCESS, statistics = statistics)
        }
      }
      CollectProjectResult(syncResult = syncResult, scope = syncWorkspace.scope)
    }
  }

  private suspend fun updateProjectModel(
    progressReporter: SequentialProgressReporter,
    storage: MutableEntityStorage,
    taskId: TaskId,
    deferredApplyActions: MutableList<suspend () -> Unit>,
  ) {
    project.syncConsole.withSubtask(
      reporter = progressReporter,
      subtaskId = taskId.subTask("apply-changes"),
      text = BazelBackendBundle.message("console.task.apply.changes"),
    ) { subtaskId ->
      val applicator = ProjectModelApplicationTask(
        project = project,
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
      text = BazelBackendBundle.message("console.task.execute.post.sync.hooks"),
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
