package org.jetbrains.bsp.protocol

sealed interface DiagnosticCode {
  @JvmInline
  value class StringValue(val value: String) : DiagnosticCode

  @JvmInline
  value class IntValue(val value: Int) : DiagnosticCode
}
