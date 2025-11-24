package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap

fun Kryo.registerGuavaSerializers() {
  addDefaultSerializer(BiMap::class.java, BiMapSerializer)
}

object BiMapSerializer : Serializer<BiMap<Any?, Any?>>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: BiMap<Any?, Any?>,
  ) {
    output.writeVarInt(obj.size, true)
    for ((key, value) in obj.entries) {
      kryo.writeClassAndObject(output, key)
      kryo.writeClassAndObject(output, value)
    }
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out BiMap<Any?, Any?>>,
  ): BiMap<Any?, Any?> {
    val size = input.readVarInt(true)
    val result = HashBiMap.create<Any?, Any?>()
    for (n in 0 until size) {
      val key = kryo.readClassAndObject(input)
      val value = kryo.readClassAndObject(input)
      result[key] = value
    }
    return result
  }

}
