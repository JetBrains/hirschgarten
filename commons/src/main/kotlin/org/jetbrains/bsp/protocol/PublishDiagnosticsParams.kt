package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class PublishDiagnosticsParams(
  val textDocument: TextDocumentIdentifier?,
  val buildTarget: Label,
  val originId: String,
  val diagnostics: List<Diagnostic>,
  val reset: Boolean,
)
