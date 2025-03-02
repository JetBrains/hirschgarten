package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class JavacOptionsItem(
  val target: Label,
  val options: List<String>,
  val classpath: List<String>,
  val classDirectory: String,
)
