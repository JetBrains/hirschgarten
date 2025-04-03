package org.jetbrains.bazel.magicmetamodel

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.ScalacOptionsItem

data class ProjectDetails(
  val targetIds: List<Label>,
  val targets: Set<BuildTarget>,
  val javacOptions: List<JavacOptionsItem>,
  val scalacOptions: List<ScalacOptionsItem>,
  val libraries: List<LibraryItem>?,
  var defaultJdkName: String? = null,
  var jvmBinaryJars: List<JvmBinaryJarsItem> = emptyList(),
)
