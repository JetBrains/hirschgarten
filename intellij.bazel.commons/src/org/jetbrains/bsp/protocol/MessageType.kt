package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class MessageType(val value: Int) {
  ERROR(1),
  WARNING(2),
  INFO(3),
  LOG(4),
}
