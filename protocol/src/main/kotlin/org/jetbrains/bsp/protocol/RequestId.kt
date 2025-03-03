package org.jetbrains.bsp.protocol

sealed interface RequestId {
  @JvmInline
  value class StringValue(val value: String) : RequestId

  @JvmInline
  value class IntValue(val value: Int) : RequestId
}
