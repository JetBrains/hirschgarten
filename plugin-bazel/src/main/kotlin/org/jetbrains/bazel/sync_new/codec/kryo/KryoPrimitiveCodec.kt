package org.jetbrains.bazel.sync_new.codec.kryo

import com.dynatrace.hash4j.hashing.HashValue128
import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import org.jetbrains.bazel.label.Label

fun Kryo.registerPrimitiveSerializers() {
  register(HashValue128::class.java, HashValue128Serializer)
  register(Label::class.java, LabelSerializer)
}

object HashValue128Serializer : Serializer<HashValue128>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: HashValue128,
  ) {
    output.writeLong(obj.mostSignificantBits)
    output.writeLong(obj.leastSignificantBits)
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out HashValue128>,
  ): HashValue128 {
    val hi = input.readLong()
    val lo = input.readLong()
    return HashValue128(hi, lo)
  }
}

object LabelSerializer : Serializer<Label>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Label,
  ) {
    output.writeString(obj.toString())
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Label>,
  ): Label {
    return Label.parse(input.readString())
  }

}
