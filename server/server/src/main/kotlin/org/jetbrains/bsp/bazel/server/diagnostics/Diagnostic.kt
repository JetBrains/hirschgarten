package org.jetbrains.bsp.bazel.server.diagnostics

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.DiagnosticSeverity

data class Diagnostic(
  val position: Position,
  val message: String,
  val fileLocation: String,
  val targetLabel: Label,
  val level: DiagnosticSeverity? = null,
)

data class Position(val line: Int, val character: Int)
