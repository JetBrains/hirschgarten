package org.jetbrains.bazel.sync.workspace.importer

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelImporterBundle
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
class WorkspaceImporterHelper(
  private val project: Project,
  private val taskConsole: TaskConsole,
  private val progressReporter: SequentialProgressReporter,
  private val taskId: TaskId,
  private val builder: MutableEntityStorage,
) {
  companion object {
    private val log = logger<WorkspaceImporterHelper>()
  }

  private val workspaceModel = WorkspaceModel.getInstance(project)
  private val context = WorkspaceImporterContext(
    project = project,
    taskConsole = taskConsole,
    progressReporter = progressReporter,
    taskId = taskId,
    vfuManager = workspaceModel.getVirtualFileUrlManager(),
    currentSnapshot = workspaceModel.currentSnapshot,
  )
  private val toSkip = mutableSetOf<BazelWorkspaceImporter>()

  suspend fun invoke(reporter: SequentialProgressReporter, snapshot: WorkspaceSnapshot) {
    taskConsole.withSubtask(
      reporter, taskId.subTask("workspace-importers"),
      BazelImporterBundle.message("bazel.workspace.importer.task.name"),
    ) { taskId ->
      taskConsole.withSubtask(
        subtaskId = taskId.subTask("workspace-importers-init"),
        message = BazelImporterBundle.message("workspace.importer.phase.initialization.progress"),
      ) { taskId ->
        BazelWorkspaceImporter.EP_NAME.extensionList.forEach { ep ->
          ep.runContextual(taskId, context, WorkspaceImporterPhase.Initialize, snapshot)
            .onFailure { toSkip += ep }
            .onSuccess { result ->
              when (result) {
                WorkspaceImporterResult.Abort -> toSkip += ep
                WorkspaceImporterResult.Success -> {
                  /* noop */
                }
              }

            }
        }
      }

      taskConsole.withSubtask(
        subtaskId = taskId.subTask("workspace-importers-wsm-building"),
        message = BazelImporterBundle.message("build.workspace.model"),
      ) { taskId ->
        BazelWorkspaceImporter.EP_NAME.extensionList.forEach { ep ->
          if (ep in toSkip) {
            return@forEach
          }
          ep.runContextual(taskId, context, WorkspaceImporterPhase.WorkspaceApply(builder, BazelProjectEntitySource), snapshot)
            .onFailure { toSkip += ep }
            .onSuccess { result ->
              when (result) {
                WorkspaceImporterResult.Abort -> toSkip += ep
                WorkspaceImporterResult.Success -> { /* noop */
                }
              }
            }
        }
      }

      taskConsole.withSubtask(
        subtaskId = taskId.subTask("workspace-importers-finalize"),
        message = BazelImporterBundle.message("workspace.importer.phase.finalization"),
      ) { taskId ->
        BazelWorkspaceImporter.EP_NAME.extensionList.forEach { ep ->
          if (ep in toSkip) {
            return@forEach
          }
          ep.runContextual(taskId, context, WorkspaceImporterPhase.Finalize, snapshot)
            .onFailure { /* noop */ }
            .onSuccess { result ->
              when (result) {
                WorkspaceImporterResult.Abort -> toSkip += ep
                WorkspaceImporterResult.Success -> {
                  /* noop */
                }
              }
            }
        }
      }
    }
  }

  suspend fun invokeLate(reporter: SequentialProgressReporter, snapshot: WorkspaceSnapshot) {
    taskConsole.withSubtask(
      reporter, taskId.subTask("workspace-importers-post-apply"),
      BazelImporterBundle.message("bazel.workspace.post.apply.task.name"),
    ) { taskId ->
      BazelWorkspaceImporter.EP_NAME.extensionList.forEach { ep ->
        if (ep in toSkip) {
          return@forEach
        }
        ep.runContextual(taskId, context, WorkspaceImporterPhase.PostProcessing, snapshot)
      }
    }
  }

  private fun <T> Result<T>.logExceptionIfNeeded(taskId: TaskId): Result<T> =
    this.onFailure { throwable ->
      log.error(throwable)
      project.syncConsole.finishSubtask(
        taskId,
        null,
        FailureResultImpl(throwable),
      )
    }

  // MAYBE RC: using context parameters for `TaskId` would be great fit here
  private suspend fun BazelWorkspaceImporter.runContextual(
    taskId: TaskId,
    context: WorkspaceImporterContext,
    phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult> = runCatching {
    if (this is BazelWorkspaceImporter.Named) {
      taskConsole.withSubtask(
        taskId.uniqueSubTask("importer"),
        BazelImporterBundle.message("workspace.importer.phase.executing", this.importerName),
      ) { taskId ->
        this.import(context.copy(taskId = taskId), phase, snapshot)
      }
    }
    else {
      this.import(context.copy(taskId = taskId), phase, snapshot)
    }
  }
    .fold(
      onSuccess = { it },
      onFailure = { Result.failure(it) },
    )
    .logExceptionIfNeeded(taskId)
}
