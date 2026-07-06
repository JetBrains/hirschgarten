package org.jetbrains.bazel.target

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.CachedValue
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.workspaceModel.ide.legacyBridge.findModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.targetKey

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class ModuleTargetService(private val project: Project) {

  private data class Indexes(
    val modulesByLabel: Map<Label, List<ModuleId>>,
    val modulesByKey: Map<WorkspaceTargetKey, List<ModuleId>>,
  )

  private val indexesCachedValue = CachedValue { storage ->
    val byLabel = hashMapOf<Label, MutableList<ModuleId>>()
    val byKey = hashMapOf<WorkspaceTargetKey, MutableList<ModuleId>>()
    for (extension in storage.entities<BazelModuleExtensionEntity>()) {
      val key = extension.targetKey
      val moduleId = extension.module.symbolicId
      byLabel.getOrPut(key.label) { ArrayList() }.add(moduleId)
      byKey.getOrPut(key) { ArrayList() }.add(moduleId)
    }
    return@CachedValue Indexes(modulesByLabel = byLabel, modulesByKey = byKey)
  }

  private val indexes
    get() = (project.workspaceModel as WorkspaceModelInternal).entityStorage.cachedValue(indexesCachedValue)

  fun findModulesByLabel(snapshot: ImmutableEntityStorage = project.workspaceModel.currentSnapshot, label: Label): List<ModuleEntity> {
    val moduleIds = indexes.modulesByLabel[label] ?: return listOf()
    return moduleIds.mapNotNull { moduleId -> snapshot.resolve(moduleId) }
  }

  fun findLegacyModulesByLabel(snapshot: ImmutableEntityStorage = project.workspaceModel.currentSnapshot, label: Label): List<Module> =
    findModulesByLabel(snapshot, label).mapNotNull { it.findModule(snapshot) }

  fun findModulesByKey(
    snapshot: ImmutableEntityStorage = project.workspaceModel.currentSnapshot,
    key: WorkspaceTargetKey,
  ): List<ModuleEntity> {
    val moduleIds = indexes.modulesByKey[key] ?: return listOf()
    return moduleIds.mapNotNull { moduleId -> snapshot.resolve(moduleId) }
  }

}
