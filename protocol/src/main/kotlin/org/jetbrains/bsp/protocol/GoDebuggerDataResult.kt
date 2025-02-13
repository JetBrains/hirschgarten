package org.jetbrains.bsp.protocol

data class GoDebuggerDataResult(
  val execRoot: String,
  val workspaceRoot: String,
  val outputBase: String,
)
