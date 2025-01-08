package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import java.net.URI

public data class LibraryItem(
  val id: BuildTargetIdentifier,
  val dependencies: List<BuildTargetIdentifier>,
  val ijars: List<String>,
  val jars: List<String>,
  val sourceJars: List<String>,
  val mavenCoordinates: MavenCoordinates?,
)

data class MavenCoordinates(
  val groupId: String,
  val artifactId: String,
  val version: String,
)

public data class GoLibraryItem(
  val id: BuildTargetIdentifier,
  val goImportPath: String? = null,
  val goRoot: URI? = null,
)

public data class WorkspaceLibrariesResult(val libraries: List<LibraryItem>)

public data class WorkspaceGoLibrariesResult(val libraries: List<GoLibraryItem>)
