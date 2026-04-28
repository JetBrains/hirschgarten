package org.jetbrains.bazel.sync.workspace.importer

import com.intellij.openapi.extensions.forEachExtensionSafeInline
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelImporterBundle
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
class WorkspaceImporterHelper(
  private val project: Project,
  private val taskConsole: TaskConsole,
  private val taskId: TaskId,
  private val builder: MutableEntityStorage,
) {
  private val workspaceModel = WorkspaceModel.getInstance(project)
  private val context = WorkspaceImporterContext(
    project = project,
    taskConsole = taskConsole,
    taskId = taskId,
    vfuManager = workspaceModel.getVirtualFileUrlManager(),
    currentSnapshot = workspaceModel.currentSnapshot,
  )
  private val toSkip = mutableSetOf<BazelWorkspaceImporter>()
  private val toApply = mutableMapOf<BazelWorkspaceImporter, MutableEntityStorage>()

  suspend fun invoke(reporter: SequentialProgressReporter, snapshot: WorkspaceSnapshot) {
    taskConsole.withSubtask(
      reporter, taskId.subTask("workspace-importers"),
      BazelImporterBundle.message("bazel.workspace.importer.task.name"),
    ) {
      // init
      BazelWorkspaceImporter.EP_NAME.forEachExtensionSafeInline { ep ->
        ep.import(context, WorkspaceImporterPhase.Initialize, snapshot)
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

      // workspace apply
      BazelWorkspaceImporter.EP_NAME.forEachExtensionSafeInline { ep ->
        if (ep in toSkip) {
          return@forEachExtensionSafeInline
        }
        val builder = MutableEntityStorage.create()
        ep.import(context, WorkspaceImporterPhase.WorkspaceApply(builder, BazelProjectEntitySource), snapshot)
          .onFailure { toSkip += ep }
          .onSuccess { result ->
            when (result) {
              WorkspaceImporterResult.Abort -> toSkip += ep
              WorkspaceImporterResult.Success -> toApply += ep to builder
            }
          }
      }

      // finalize
      BazelWorkspaceImporter.EP_NAME.forEachExtensionSafeInline { ep ->
        if (ep in toSkip) {
          return@forEachExtensionSafeInline
        }
        ep.import(context, WorkspaceImporterPhase.Finalize, snapshot)
          .onFailure { toApply -= ep }
          .onSuccess { result ->
            when (result) {
              WorkspaceImporterResult.Abort -> toSkip += ep
              WorkspaceImporterResult.Success -> {
                /* noop */
              }
            }
          }
      }

      toApply.forEach { (_, storage) -> builder.applyChangesFrom(storage) }
    }
  }

  suspend fun invokeLate(reporter: SequentialProgressReporter, snapshot: WorkspaceSnapshot) {
    taskConsole.withSubtask(
      reporter, taskId.subTask("workspace-importers"),
      BazelImporterBundle.message("bazel.workspace.post.apply.task.name"),
    ) {
      BazelWorkspaceImporter.EP_NAME.forEachExtensionSafeInline { ep ->
        if (ep in toSkip) {
          return@forEachExtensionSafeInline
        }
        ep.import(context, WorkspaceImporterPhase.PostProcessing, snapshot)
      }
    }
  }
}
