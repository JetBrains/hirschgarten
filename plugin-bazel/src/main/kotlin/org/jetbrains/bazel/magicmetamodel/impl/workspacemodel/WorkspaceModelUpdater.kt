package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Library
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.RawBuildTarget

data class ModuleDetails(
  val target: RawBuildTarget,
  val javacOptions: JavacOptionsItem?,
  val libraryDependencies: List<CanonicalLabel>?,
  val moduleDependencies: List<CanonicalLabel>,
  val defaultJdkName: String?,
  val jvmBinaryJars: List<JvmBinaryJarsItem>,
)

interface WorkspaceModelUpdater {
  suspend fun loadModules(moduleEntities: List<Module>, libraryModules: List<JavaModule>)

  suspend fun loadLibraries(libraries: List<Library>)

  suspend fun loadCompiledSourceCodeInsideJarExclude(exclude: CompiledSourceCodeInsideJarExclude)
}
