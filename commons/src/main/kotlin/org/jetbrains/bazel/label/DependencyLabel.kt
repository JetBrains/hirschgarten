package org.jetbrains.bazel.label

import org.jetbrains.annotations.ApiStatus

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
