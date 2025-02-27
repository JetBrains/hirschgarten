package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel

import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.ScalacOptionsItem
import org.jetbrains.bsp.protocol.SourcesItem

data class ModuleDetails(
  val target: BuildTarget,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
  val javacOptions: JavacOptionsItem?,
  val scalacOptions: ScalacOptionsItem?,
  val outputPathUris: List<String>,
  val libraryDependencies: List<BuildTargetIdentifier>?,
  val moduleDependencies: List<BuildTargetIdentifier>,
  val defaultJdkName: String?,
  val jvmBinaryJars: List<JvmBinaryJarsItem>,
  val workspaceModelEntitiesFolderMarker: Boolean = false,
)

interface WorkspaceModelUpdater {
  suspend fun loadModules(moduleEntities: List<Module>)

  suspend fun loadLibraries(libraries: List<Library>)

  suspend fun loadCompiledSourceCodeInsideJarExclude(exclude: CompiledSourceCodeInsideJarExclude)
}
