package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
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
