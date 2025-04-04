package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import java.nio.file.Path

data class Diagnostic(
  val position: Position,
  val message: String,
  val fileLocation: Path?,
  val targetLabel: Label,
  val level: DiagnosticSeverity? = null,
)

data class Position(val line: Int, val character: Int)
