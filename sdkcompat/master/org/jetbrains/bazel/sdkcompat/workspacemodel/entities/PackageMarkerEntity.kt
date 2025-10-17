package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModifiableModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

interface PackageMarkerEntity : WorkspaceEntity {
  val root: VirtualFileUrl
  val packagePrefix: String

  @Parent
  val module: ModuleEntity
}

val ModuleEntity.packageMarkerEntities: List<PackageMarkerEntity>
  by WorkspaceEntity.extension()
