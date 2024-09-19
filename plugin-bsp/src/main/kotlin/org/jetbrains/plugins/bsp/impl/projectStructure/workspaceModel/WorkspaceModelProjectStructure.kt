package org.jetbrains.plugins.bsp.projectStructure.workspaceModel

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
import org.jetbrains.plugins.bsp.building.syncConsole
import org.jetbrains.plugins.bsp.building.withSubtask
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.impl.flow.sync.FullProjectSync
import org.jetbrains.plugins.bsp.impl.flow.sync.PartialProjectSync
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncScope
import org.jetbrains.plugins.bsp.impl.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.impl.utils.orDefault
import org.jetbrains.plugins.bsp.performance.bspTracer
import org.jetbrains.plugins.bsp.projectStructure.AllProjectStructuresDiff
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureDiff
import org.jetbrains.plugins.bsp.projectStructure.ProjectStructureProvider
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspEntitySource
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspModuleEntitySource
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

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
      message = BspPluginBundle.message("console.task.model.apply.changes"),
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
    val moduleNameProvider = project.findModuleNameProvider().orDefault()
    val targetsToSyncNames = syncScope.targetsToSync.map { moduleNameProvider(BuildTargetInfo(id = it)) }

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
