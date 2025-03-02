package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class ScalaTestClassesItem(
  val target: Label,
  val classes: List<String>,
  val framework: String? = null,
)
