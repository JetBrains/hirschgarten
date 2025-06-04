package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem

data class ModuleDetails(
  val target: BuildTarget,
  val javacOptions: JavacOptionsItem?,
  val libraryDependencies: List<Label>?,
  val moduleDependencies: List<Label>,
  val defaultJdkName: String?,
  val jvmBinaryJars: List<JvmBinaryJarsItem>,
)

interface WorkspaceModelUpdater {
  suspend fun loadModules(moduleEntities: List<Module>)

  suspend fun loadLibraries(libraries: List<Library>)

  suspend fun loadCompiledSourceCodeInsideJarExclude(exclude: CompiledSourceCodeInsideJarExclude)
}
