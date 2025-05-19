package org.jetbrains.bazel.sync.projectStructure.workspaceModel

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.workspace.jps.JpsFileDependentEntitySource
import com.intellij.platform.workspace.jps.JpsFileEntitySource
import com.intellij.platform.workspace.jps.JpsGlobalFileEntitySource
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.sdkcompat.replaceWorkspaceModelCompat
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresDiff
import org.jetbrains.bazel.sync.projectStructure.ProjectStructureDiff
import org.jetbrains.bazel.sync.projectStructure.ProjectStructureProvider
import org.jetbrains.bazel.sync.scope.FullProjectSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource

private const val MAX_REPLACE_WSM_ATTEMPTS = 3

class WorkspaceModelProjectStructureDiff(val mutableEntityStorage: MutableEntityStorage) : ProjectStructureDiff {
  private val postApplyActions = mutableListOf<suspend () -> Unit>()

  fun addPostApplyAction(action: suspend () -> Unit) {
    postApplyActions.add(action)
  }

  override suspend fun apply(
    project: Project,
    syncScope: ProjectSyncScope,
    taskId: String,
  ) {
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "apply-changes-on-workspace-model",
      message = BazelPluginBundle.message("console.task.model.apply.changes"),
    ) { subtaskId ->
      bspTracer.spanBuilder("apply.changes.on.workspace.model.ms").useWithScope {
        var workspaceModelUpdated = false
        val workspaceModel = WorkspaceModel.getInstance(project) as WorkspaceModelInternal
        repeat(MAX_REPLACE_WSM_ATTEMPTS) { attemptIdx ->
          val snapshot = workspaceModel.getBuilderSnapshot()
          bspTracer.spanBuilder("replacebysource.in.apply.on.workspace.model.ms").use {
            snapshot.builder.replaceBySource({ it.isBazelRelevant(project, syncScope) }, mutableEntityStorage)
          }
          // quickly return if there are no changes to apply
          if (!snapshot.areEntitiesChanged()) return@useWithScope
          val storageReplacement = snapshot.getStorageReplacement()
          workspaceModelUpdated =
            writeAction {
              bspTracer.spanBuilder("replaceprojectmodel.in.apply.on.workspace.model.ms").use {
                workspaceModel.replaceWorkspaceModelCompat(
                  BazelPluginBundle.message("console.task.model.apply.changes.attempt.0.1.wsm", attemptIdx + 1, MAX_REPLACE_WSM_ATTEMPTS),
                  storageReplacement,
                )
              }
            }
          if (workspaceModelUpdated) return@useWithScope
          project.syncConsole.addMessage(
            subtaskId,
            BazelPluginBundle.message("console.task.model.apply.changes.attempt.0.1.failed", attemptIdx + 1, MAX_REPLACE_WSM_ATTEMPTS),
          )
        }
        if (!workspaceModelUpdated) {
          project.syncConsole.addMessage(
            subtaskId,
            BazelPluginBundle.message("console.task.model.apply.changes.attempt.0.fallback", MAX_REPLACE_WSM_ATTEMPTS),
          )
          workspaceModel.update(BazelPluginBundle.message("console.task.model.apply.changes.wsm")) { builder ->
            builder.replaceBySource({ it.isBazelRelevant(project, syncScope) }, mutableEntityStorage)
          }
        }
      }
    }

    postApplyActions.forEach { it() }
  }

  private fun EntitySource.isBazelRelevant(project: Project, syncScope: ProjectSyncScope): Boolean =
    when (syncScope) {
      is FullProjectSync -> isBazelRelevantForFullSync()
      is PartialProjectSync -> isBazelRelevantForPartialSync(project, syncScope)
    }

  private fun EntitySource.isBazelRelevantForPartialSync(project: Project, syncScope: PartialProjectSync): Boolean {
    val targetsToSyncNames = syncScope.targetsToSync.map { it.formatAsModuleName(project) }

    if (this is BazelModuleEntitySource) {
      return moduleName in targetsToSyncNames
    }

    return false
  }

  private fun EntitySource.isBazelRelevantForFullSync(): Boolean =
    when (this) {
      // avoid touching global sources
      is JpsGlobalFileEntitySource -> false

      is JpsFileEntitySource,
      is JpsFileDependentEntitySource,
      is BazelEntitySource,
      -> true

      else -> false
    }
}

val AllProjectStructuresDiff.workspaceModelDiff: WorkspaceModelProjectStructureDiff
  get() = diffOfType(WorkspaceModelProjectStructureDiff::class.java)

class WorkspaceModelProjectStructureProvider : ProjectStructureProvider<WorkspaceModelProjectStructureDiff> {
  override fun newDiff(project: Project): WorkspaceModelProjectStructureDiff =
    WorkspaceModelProjectStructureDiff(MutableEntityStorage.create())
}
