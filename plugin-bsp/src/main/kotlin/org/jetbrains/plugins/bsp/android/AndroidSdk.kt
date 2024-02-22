package org.jetbrains.plugins.bsp.android

import java.nio.file.Path

public data class AndroidSdk(
  val name: String,
  val androidJar: Path,
)
