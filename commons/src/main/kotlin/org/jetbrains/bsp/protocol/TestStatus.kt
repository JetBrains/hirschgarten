package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class TestStatus(val value: Int) {
  PASSED(1),
  FAILED(2),
  IGNORED(3),
  CANCELLED(4),
  SKIPPED(5),
}
