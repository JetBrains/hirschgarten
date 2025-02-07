package org.jetbrains.bsp.bazel.server.model

import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.net.URI

data class Library(
  val label: Label,
  val outputs: Set<URI>,
  val sources: Set<URI>,
  val dependencies: List<Label>,
  val interfaceJars: Set<URI> = emptySet(),
  val mavenCoordinates: MavenCoordinates? = null,
)

data class GoLibrary(
  val label: Label,
  val goImportPath: String? = null,
  val goRoot: URI? = null,
)
