package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class CppOptionsItem(
  val target: Label,
  val copts: List<String>,
  val defines: List<String>,
  val linkopts: List<String>,
  val linkshared: Boolean? = null,
)
