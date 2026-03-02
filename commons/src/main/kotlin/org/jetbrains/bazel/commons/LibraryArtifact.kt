package org.jetbrains.bazel.commons

internal data class LibraryArtifact(
  val interfaceJar: ArtifactLocation?,
  val classJar: ArtifactLocation?,
  val sourceJars: List<ArtifactLocation>,
)
