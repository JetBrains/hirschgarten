package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType

@ApiStatus.Internal
interface BazelModuleExtensionEntity : WorkspaceEntity {
  @Parent
  val module: ModuleEntity

  val rootTypeId: WorkspaceModelTargetSourceRootTypeId
  val label: WorkspaceModelTargetLabel
  val strictDependencies: WorkspaceModelTargetLabelList
}

@ApiStatus.Internal
interface BazelLibraryExtensionEntity : WorkspaceEntity {
  @Parent
  val library: LibraryEntity

  val label: WorkspaceModelTargetLabel
  val isSynthetic: Boolean
}

@get:ApiStatus.Internal
val ModuleEntity.bazelModuleExtension: BazelModuleExtensionEntity? by WorkspaceEntity.extension()

@get:ApiStatus.Internal
val LibraryEntity.bazelLibraryExtension: BazelLibraryExtensionEntity? by WorkspaceEntity.extension()

@ApiStatus.Internal
class WorkspaceModelTargetLabel(private val label: String) {
  constructor(label: Label) : this(label.toString())

  fun toLabel(): Label = Label.parse(label)
}

@ApiStatus.Internal
class WorkspaceModelTargetLabelList(val check: StrictDependencyCheckedType, val labels: List<String>)

@ApiStatus.Internal
class WorkspaceModelTargetSourceRootTypeId(val default: SourceRootTypeId?)
