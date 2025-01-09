package org.jetbrains.bsp.bazel.server.diagnostics

import ch.epfl.scala.bsp4j.DiagnosticSeverity
import org.jetbrains.bazel.commons.label.Label

data class Diagnostic(
  val position: Position,
  val message: String,
  val fileLocation: String,
  val targetLabel: Label,
  val level: DiagnosticSeverity? = null,
)

data class Position(val line: Int, val character: Int)
