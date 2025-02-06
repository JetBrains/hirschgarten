package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import java.net.URI

data class GoBuildTarget(
  val sdkHomePath: URI?,
  val importPath: String?,
  val generatedLibraries: List<URI>,
  val generatedSources: List<URI> = emptyList(),
  // Bazel specific
  val ruleKind: String = "",
  val libraryLabels: List<BuildTargetIdentifier> = emptyList(),
)
