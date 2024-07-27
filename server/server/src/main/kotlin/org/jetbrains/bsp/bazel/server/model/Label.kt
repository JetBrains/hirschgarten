package org.jetbrains.bsp.bazel.server.model

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer

@JvmInline
value class Label private constructor(
  @JsonValue val value: String,
) {
  fun targetName(): String = value.substringAfterLast(":", "")

  override fun toString(): String = value

  companion object {
    fun parse(value: String): Label = Label(value.intern())
  }
}

class LabelKeyDeserializer : KeyDeserializer() {
  override fun deserializeKey(key: String, ctxt: DeserializationContext): Label = Label.parse(key)
}
