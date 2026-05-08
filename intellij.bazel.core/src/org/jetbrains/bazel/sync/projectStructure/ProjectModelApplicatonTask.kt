package org.jetbrains.bazel.sync.projectStructure

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.diagnostic.telemetry.helpers.useWithScope
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.entities
import com.intellij.workspaceModel.ide.impl.WorkspaceModelImpl
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.scope.FullProjectSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.workspacemodel.entities.BazelEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bazel.workspacemodel.entities.bazelLibraryExtension
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import org.jetbrains.bsp.protocol.TaskId

internal class ProjectModelApplicatonTask(
  private val project: Project,
  private val scope: ProjectSyncScope,
  private val taskId: TaskId,
  private val postActions: List<suspend () -> Unit>,
  private val targetsToReplace: Set<Label> = emptySet(),
) {
  companion object {
    private const val MAX_REPLACE_WSM_ATTEMPTS = 3
  }

  suspend fun apply(storage: MutableEntityStorage) {
    fun MutableEntityStorage.replaceAllBazelEntities() {
      replaceBySource(
        sourceFilter = { entitySource -> entitySource is BazelEntitySource },
        replaceWith = storage,
      )
    }

    fun MutableEntityStorage.replacePartialBazelEntities() {
      val targetModuleNames = targetsToReplace.map { it.formatAsModuleName(project) }.toSet()
      removeTargetEntities(targetsToReplace, targetModuleNames)
      storage.keepOnlyTargetEntities(targetsToReplace, targetModuleNames)
      applyChangesFrom(storage)
    }

    project.syncConsole.withSubtask(
      subtaskId = taskId.subTask("apply-changes-on-workspace-model"),
      message = BazelPluginBundle.message("console.task.model.apply.changes"),
    ) {
      bspTracer.spanBuilder("apply.changes.on.workspace.model.ms").useWithScope {
        val workspaceModel = project.serviceAsync<WorkspaceModel>() as WorkspaceModelImpl
        workspaceModel.updateWithRetry(
          BazelPluginBundle.message("console.task.model.apply.changes.attempt.0.1.wsm", 0, 0),
          MAX_REPLACE_WSM_ATTEMPTS,
        ) { builder ->
          bspTracer.spanBuilder("replaceprojectmodel.in.apply.on.workspace.model.ms").use {
            when (scope) {
              is FullProjectSync -> builder.replaceAllBazelEntities()
              is PartialProjectSync -> builder.replacePartialBazelEntities()
            }
          }
        }
      }
    }

    postActions.forEach { it() }
  }

  private fun MutableEntityStorage.removeTargetEntities(targetLabels: Set<Label>, targetModuleNames: Set<String>) {
    entities(ModuleEntity::class.java)
      .filter { it.isTargetModule(targetLabels, targetModuleNames) }
      .toList()
      .forEach { removeEntity(it) }

    entities(LibraryEntity::class.java)
      .filter { it.isTargetLibrary(targetLabels, targetModuleNames) }
      .toList()
      .forEach { removeEntity(it) }
  }

  private fun MutableEntityStorage.keepOnlyTargetEntities(targetLabels: Set<Label>, targetModuleNames: Set<String>) {
    entities(ModuleEntity::class.java)
      .filterNot { it.isTargetModule(targetLabels, targetModuleNames) }
      .toList()
      .forEach { removeEntity(it) }

    removeEntities<BazelProjectDirectoriesEntity>()
    removeEntities<CompiledSourceCodeInsideJarExcludeEntity>()
    removeEntities<LibraryCompiledSourceCodeInsideJarExcludeEntity>()
  }

  private inline fun <reified T : WorkspaceEntity> MutableEntityStorage.removeEntities() {
    entities(T::class.java).toList().forEach { removeEntity(it) }
  }

  private fun ModuleEntity.isTargetModule(targetLabels: Set<Label>, targetModuleNames: Set<String>): Boolean =
    bazelModuleExtension?.label?.toLabel()?.let { it in targetLabels } == true ||
      (entitySource as? BazelModuleEntitySource)?.moduleName?.let { it in targetModuleNames } == true

  private fun LibraryEntity.isTargetLibrary(targetLabels: Set<Label>, targetModuleNames: Set<String>): Boolean =
    bazelLibraryExtension?.label?.toLabel()?.let { it in targetLabels } == true ||
      (entitySource as? BazelModuleEntitySource)?.moduleName?.let { it in targetModuleNames } == true
}
