package org.jetbrains.bazel.server.diagnostics

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import org.jetbrains.bsp.protocol.Position
import java.nio.file.Path

@ApiStatus.Internal
data class Diagnostic(
  val position: Position,
  val message: String,
  val fileLocation: Path?,
  val targetLabel: Label,
  val level: DiagnosticSeverity? = null,
)
