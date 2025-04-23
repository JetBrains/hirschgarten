package org.jetbrains.bsp.protocol

enum class StatusCode(val value: Int) {
  OK(1),
  ERROR(2),
  CANCELLED(3),
}
