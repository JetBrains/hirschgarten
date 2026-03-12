package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class Diagnostic(
  val range: Range,
  val message: String,
  val severity: DiagnosticSeverity? = null,
  val code: DiagnosticCode? = null,
  val codeDescription: CodeDescription? = null,
  val source: String? = null,
  val tags: List<Int>? = null,
  val relatedInformation: List<DiagnosticRelatedInformation>? = null,
)
