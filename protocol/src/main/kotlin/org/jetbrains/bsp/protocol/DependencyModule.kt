package org.jetbrains.bsp.protocol

data class DependencyModule(
  val name: String,
  val version: String,
  val dependencyModule: MavenDependencyModule? = null,
)

data class MavenDependencyModule(
  val organization: String,
  val name: String,
  val version: String,
  val artifacts: List<MavenDependencyModuleArtifact>,
  val scope: String? = null,
)

data class MavenDependencyModuleArtifact(val uri: String, val classifier: String? = null)
