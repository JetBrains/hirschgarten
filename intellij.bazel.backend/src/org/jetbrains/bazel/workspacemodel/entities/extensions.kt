package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType

@ApiStatus.Internal
interface BazelModuleExtensionEntity : WorkspaceEntity {
  @Parent
  val module: ModuleEntity

  val _targetKey: WorkspaceModelTargetKey
  val rootTypeId: WorkspaceModelTargetSourceRootTypeId
  val strictDependencies: WorkspaceModelTargetLabelList
}

@ApiStatus.Internal
interface BazelLibraryExtensionEntity : WorkspaceEntity {
  @Parent
  val library: LibraryEntity

  val _targetKey: WorkspaceModelTargetKey
  val isSynthetic: Boolean
}

@get:ApiStatus.Internal
val ModuleEntity.bazelModuleExtension: BazelModuleExtensionEntity? by WorkspaceEntity.extension()

@get:ApiStatus.Internal
val LibraryEntity.bazelLibraryExtension: BazelLibraryExtensionEntity? by WorkspaceEntity.extension()

@ApiStatus.Internal
data class WorkspaceModelTargetKey(
  private val label: String,
  private val configuration: String?,
  internal val aspectIds: List<String>,
) {
  fun toWorkspaceTarget(): WorkspaceTargetKey = WorkspaceTargetKey(
    label = Label.parse(label),
    configuration = WorkspaceConfigurationId.of(configuration),
    aspectIds = WorkspaceAspectIds.of(aspectIds),
  )

  companion object {
    fun of(key: WorkspaceTargetKey): WorkspaceModelTargetKey = WorkspaceModelTargetKey(
      label = key.label.toString(),
      configuration = key.configuration.shortChecksum,
      aspectIds = key.aspectIds.ids,
    )
  }
}

@get:ApiStatus.Internal
val BazelModuleExtensionEntity.targetKey: WorkspaceTargetKey
  get() = _targetKey.toWorkspaceTarget()

@get:ApiStatus.Internal
val BazelLibraryExtensionEntity.targetKey: WorkspaceTargetKey
  get() = _targetKey.toWorkspaceTarget()

@ApiStatus.Internal
class WorkspaceModelTargetLabelList(val check: StrictDependencyCheckedType, val labels: List<String>)

@ApiStatus.Internal
class WorkspaceModelTargetSourceRootTypeId(val default: SourceRootTypeId?)
