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
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresDiff
import org.jetbrains.bazel.sync.projectStructure.ProjectStructureDiff
import org.jetbrains.bazel.sync.projectStructure.ProjectStructureProvider
import org.jetbrains.bazel.sync.scope.FullProjectSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bazel.workspacemodel.entities.BspEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BspModuleEntitySource

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
    ) {
      bspTracer.spanBuilder("apply.changes.on.workspace.model.ms").useWithScope {
        val workspaceModel = WorkspaceModel.getInstance(project) as WorkspaceModelInternal
        val snapshot = workspaceModel.getBuilderSnapshot()
        bspTracer.spanBuilder("replacebysource.in.apply.on.workspace.model.ms").use {
          snapshot.builder.replaceBySource({ it.isBspRelevant(project, syncScope) }, mutableEntityStorage)
        }
        val storageReplacement = snapshot.getStorageReplacement()
        writeAction {
          val workspaceModelUpdated =
            bspTracer.spanBuilder("replaceprojectmodel.in.apply.on.workspace.model.ms").use {
              workspaceModel.replaceProjectModel(storageReplacement)
            }
          if (!workspaceModelUpdated) {
            error("Project model is not updated successfully. Try `reload` action to recalculate the project model.")
          }
        }
      }
    }

    postApplyActions.forEach { it() }
  }

  private fun EntitySource.isBspRelevant(project: Project, syncScope: ProjectSyncScope): Boolean =
    when (syncScope) {
      is FullProjectSync -> isBspRelevantForFullSync()
      is PartialProjectSync -> isBspRelevantForPartialSync(project, syncScope)
    }

  private fun EntitySource.isBspRelevantForPartialSync(project: Project, syncScope: PartialProjectSync): Boolean {
    val targetsToSyncNames = syncScope.targetsToSync.map { it.formatAsModuleName(project) }

    if (this is BspModuleEntitySource) {
      return moduleName in targetsToSyncNames
    }

    return false
  }

  private fun EntitySource.isBspRelevantForFullSync(): Boolean =
    when (this) {
      // avoid touching global sources
      is JpsGlobalFileEntitySource -> false

      is JpsFileEntitySource,
      is JpsFileDependentEntitySource,
      is BspEntitySource,
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
