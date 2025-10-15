package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel

import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Library
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

data class ModuleDetails(
  val target: RawBuildTarget,
  val javacOptions: List<String>,
  val dependencies: List<DependencyLabel>,
  val defaultJdkName: String?,
  val jvmBinaryJars: List<Path>,
)

interface WorkspaceModelUpdater {
  suspend fun loadModules(moduleEntities: List<Module>, libraryModules: List<JavaModule>)

  suspend fun loadLibraries(libraries: List<Library>)

  suspend fun loadCompiledSourceCodeInsideJarExclude(exclude: CompiledSourceCodeInsideJarExclude)
}
