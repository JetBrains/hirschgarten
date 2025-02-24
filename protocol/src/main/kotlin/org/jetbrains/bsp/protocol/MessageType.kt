package org.jetbrains.bsp.protocol

enum class MessageType(val value: Int) {
  ERROR(1),
  WARNING(2),
  INFO(3),
  LOG(4),
}
