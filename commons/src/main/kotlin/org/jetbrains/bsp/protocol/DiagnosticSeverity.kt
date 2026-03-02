package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class DiagnosticSeverity(val value: Int) {
  ERROR(1),
  WARNING(2),
  INFORMATION(3),
  HINT(4),
}
