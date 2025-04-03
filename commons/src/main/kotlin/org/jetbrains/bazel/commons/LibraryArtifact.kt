package org.jetbrains.bazel.commons

data class LibraryArtifact(
  val interfaceJar: ArtifactLocation?,
  val classJar: ArtifactLocation?,
  val sourceJars: List<ArtifactLocation>,
)
