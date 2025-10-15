package org.jetbrains.bazel.label

data class DependencyLabel(
  val label: Label,
  val isRuntime: Boolean = false,
)
