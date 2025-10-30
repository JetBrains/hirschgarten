package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.annotations.PublicApi

@PublicApi
interface ScalaAddendumEntity : WorkspaceEntity {
  val compilerVersion: String
  val scalacOptions: List<String>
  val sdkClasspaths: List<VirtualFileUrl>

  @Parent
  val module: ModuleEntity
}

@PublicApi
val ModuleEntity.scalaAddendumEntity: ScalaAddendumEntity? by WorkspaceEntity.extension()
