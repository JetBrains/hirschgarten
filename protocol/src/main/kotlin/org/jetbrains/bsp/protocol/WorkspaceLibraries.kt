package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

public data class LibraryItem(
  val id: Label,
  val dependencies: List<Label>,
  val ijars: List<Path>,
  val jars: List<Path>,
  val sourceJars: List<Path>,
  val mavenCoordinates: MavenCoordinates?,
)

data class MavenCoordinates(
  val groupId: String,
  val artifactId: String,
  val version: String,
)

public data class GoLibraryItem(
  val id: Label,
  val goImportPath: String? = null,
  val goRoot: Path? = null,
)

public data class WorkspaceLibrariesResult(val libraries: List<LibraryItem>)

public data class WorkspaceGoLibrariesResult(val libraries: List<GoLibraryItem>)
