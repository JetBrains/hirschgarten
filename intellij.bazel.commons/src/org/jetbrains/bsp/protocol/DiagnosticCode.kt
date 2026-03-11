package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
sealed interface DiagnosticCode {
  @JvmInline
  value class StringValue(val value: String) : DiagnosticCode

  @JvmInline
  value class IntValue(val value: Int) : DiagnosticCode
}
