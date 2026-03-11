package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

internal interface JvmBinaryJarsEntity : WorkspaceEntity {
  val jars: List<VirtualFileUrl>

  @Parent
  val module: ModuleEntity
}

internal val ModuleEntity.jvmBinaryJarsEntity: JvmBinaryJarsEntity? by WorkspaceEntity.extension()
