package org.jetbrains.bazel.server.model

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path

data class Library(
  val label: Label,
  val outputs: Set<Path>,
  val sources: Set<Path>,
  val dependencies: List<Label>,
  val interfaceJars: Set<Path> = emptySet(),
  val mavenCoordinates: MavenCoordinates? = null,
)

data class GoLibrary(
  val label: Label,
  val goImportPath: String? = null,
  val goRoot: Path? = null,
)
