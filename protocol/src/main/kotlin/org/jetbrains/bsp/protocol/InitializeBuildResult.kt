package org.jetbrains.bsp.protocol

data class InitializeBuildResult(
  val displayName: String,
  val version: String,
  val bspVersion: String,
)
