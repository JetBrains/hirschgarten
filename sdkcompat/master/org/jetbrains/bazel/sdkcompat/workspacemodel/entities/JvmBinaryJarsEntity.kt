package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.annotations.PublicApi

@PublicApi
interface JvmBinaryJarsEntity : WorkspaceEntity {
  val jars: List<VirtualFileUrl>

  @Parent
  val module: ModuleEntity
}

@PublicApi
val ModuleEntity.jvmBinaryJarsEntity: JvmBinaryJarsEntity? by WorkspaceEntity.extension()
