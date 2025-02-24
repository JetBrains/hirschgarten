package org.jetbrains.bsp.protocol

data class PublishDiagnosticsParams(
  val textDocument: TextDocumentIdentifier,
  val buildTarget: BuildTargetIdentifier,
  val originId: String,
  val diagnostics: List<Diagnostic>,
  val reset: Boolean,
)
