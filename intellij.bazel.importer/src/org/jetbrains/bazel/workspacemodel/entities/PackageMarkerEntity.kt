package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.ApiStatus


@ApiStatus.Internal
interface PackageMarkerEntity : WorkspaceEntity {
  val root: VirtualFileUrl
  val packagePrefix: String

  @Parent
  val module: ModuleEntity
}

@get:ApiStatus.Internal
val ModuleEntity.packageMarkerEntities: List<PackageMarkerEntity>
  by WorkspaceEntity.extension()
