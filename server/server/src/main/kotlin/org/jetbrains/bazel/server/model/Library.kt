package org.jetbrains.bazel.server.model

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path

data class Library(
  val label: CanonicalLabel,
  val outputs: Set<Path>,
  val sources: Set<Path>,
  val dependencies: List<CanonicalLabel>,
  val interfaceJars: Set<Path> = emptySet(),
  val mavenCoordinates: MavenCoordinates? = null,
  val isFromInternalTarget: Boolean = false,
)

data class GoLibrary(
  val label: CanonicalLabel,
  val goImportPath: String? = null,
  val goRoot: Path? = null,
)
