package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

internal interface ScalaAddendumEntity : WorkspaceEntity {
  val compilerVersion: String
  val scalacOptions: List<String>
  val sdkClasspaths: List<VirtualFileUrl>

  @Parent
  val module: ModuleEntity
}

internal val ModuleEntity.scalaAddendumEntity: ScalaAddendumEntity? by WorkspaceEntity.extension()
