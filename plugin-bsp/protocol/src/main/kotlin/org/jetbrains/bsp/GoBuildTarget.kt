package org.jetbrains.bsp

import java.net.URI

public data class GoBuildTarget(
  val sdkHomePath: URI?,
  val importPath: String?,
)
