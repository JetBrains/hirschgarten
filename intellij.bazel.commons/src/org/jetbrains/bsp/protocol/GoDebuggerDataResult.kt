package org.jetbrains.bsp.protocol

internal data class GoDebuggerDataResult(
  val execRoot: String,
  val workspaceRoot: String,
  val outputBase: String,
)
