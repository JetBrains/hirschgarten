package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExclude
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType
import java.nio.file.Path

@ApiStatus.Internal
data class ModuleDetails(
  val target: RawBuildTarget,
  val javacOptions: List<String>,
  val dependencies: List<DependencyLabel>,
  val strictDependenciesCheck: StrictDependencyCheckedType,
  val strictDependencies: List<Label>,
  val defaultJdkName: String?,
  val jvmBinaryJars: List<Path>,
)
