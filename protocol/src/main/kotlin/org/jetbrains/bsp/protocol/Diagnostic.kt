package org.jetbrains.bsp.protocol

data class Diagnostic(
  val range: Range,
  val severity: DiagnosticSeverity? = null,
  val code: DiagnosticCode? = null,
  val codeDescription: CodeDescription? = null,
  val source: String? = null,
  val message: String,
  val tags: List<Int>? = null,
  val relatedInformation: List<DiagnosticRelatedInformation>? = null,
)
