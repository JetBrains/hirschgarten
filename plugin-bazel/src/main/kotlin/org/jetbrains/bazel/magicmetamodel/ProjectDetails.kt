package org.jetbrains.bazel.magicmetamodel

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JvmBinaryJarsItem
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

data class ProjectDetails(
  val targetIds: List<CanonicalLabel>,
  val targets: Set<RawBuildTarget>,
  val javacOptions: List<JavacOptionsItem>,
  val libraries: List<LibraryItem>?,
  var defaultJdkName: String? = null,
  var jvmBinaryJars: List<JvmBinaryJarsItem> = emptyList(),
)
