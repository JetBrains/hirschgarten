package org.jetbrains.bazel.label

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.info.BspTargetInfo

@ApiStatus.Internal
data class DependencyLabel(
  val label: Label,
  val isRuntime: Boolean = false,
  val exported: Boolean = false,
) {
  companion object {
    fun parse(value: String, isRuntime: Boolean = false): DependencyLabel =
      DependencyLabel(Label.parse(value), isRuntime)
  }
}

@ApiStatus.Internal
fun BspTargetInfo.Dependency.toDependencyLabel(): DependencyLabel =
  DependencyLabel(
    label = label(),
    isRuntime = dependencyType == BspTargetInfo.Dependency.DependencyType.RUNTIME,
    exported =  dependencyType == BspTargetInfo.Dependency.DependencyType.EXPORTED_COMPILE_TIME,
  )
