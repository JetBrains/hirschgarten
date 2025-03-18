package org.jetbrains.bazel.label

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LabelSerializer : KSerializer<Label> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("Label", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Label) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): Label {
    val labelString = decoder.decodeString()
    return Label.parse(labelString)
  }
}
