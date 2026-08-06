package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.diagnostics.Diagnostic

data class PublishDiagnosticsParams(
  val taskId: TaskId,
  val textDocument: TextDocumentIdentifier?,
  val buildTarget: Label,
  val diagnostics: List<Diagnostic>,
  val reset: Boolean,
)
