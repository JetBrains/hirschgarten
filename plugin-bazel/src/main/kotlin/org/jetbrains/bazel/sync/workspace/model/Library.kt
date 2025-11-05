package org.jetbrains.bazel.sync.workspace.model

import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path

data class Library(
  val label: Label,
  val outputs: Set<Path>,
  val sources: Set<Path>,
  val dependencies: List<DependencyLabel>,
  val interfaceJars: Set<Path> = emptySet(),
  val mavenCoordinates: MavenCoordinates? = null,
  val isFromInternalTarget: Boolean = false,
  val isLowPriority: Boolean = false,
)
