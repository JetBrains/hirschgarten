package org.jetbrains.bsp.protocol

import java.net.URI

data class GoBuildTarget(
  val sdkHomePath: URI?,
  val importPath: String,
  val generatedLibraries: List<URI>,
)
