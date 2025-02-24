package org.jetbrains.bsp.protocol

enum class TestStatus(val value: Int) {
  PASSED(1),
  FAILED(2),
  IGNORED(3),
  CANCELLED(4),
  SKIPPED(5),
}
