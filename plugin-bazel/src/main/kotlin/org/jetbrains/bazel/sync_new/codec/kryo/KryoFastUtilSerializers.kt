package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.longs.LongSet
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap

// TODO: invent more generic way of serializing fastutil collections
fun Kryo.registerFastUtilSerializers() {
  addDefaultSerializer(LongList::class.java, KryoLongListSerializer)
  addDefaultSerializer(LongSet::class.java, KryoLongSetSerializer)

  addDefaultSerializer(Long2ObjectMap::class.java, Long2ObjectMapSerializer)
  addDefaultSerializer(Object2LongMap::class.java, Object2LongMapSerializer)
}

object KryoLongListSerializer : Serializer<LongList>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: LongList,
  ) {
    output.writeVarInt(obj.size, true)
    if (obj is LongArrayList) {
      output.writeLongs(obj.elements(), 0, obj.size)
    } else {
      for (n in 0 until obj.size) {
        output.writeLong(obj.getLong(n))
      }
    }
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out LongList>,
  ): LongList {
    val length = input.readVarInt(true)
    return LongArrayList(input.readLongs(length))
  }
}

object KryoLongSetSerializer : Serializer<LongSet>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: LongSet,
  ) {
    output.writeVarInt(obj.size, true)
    val iterator = obj.iterator()
    while (iterator.hasNext()) {
      output.writeLong(iterator.nextLong())
    }
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out LongSet>,
  ): LongSet {
    val length = input.readVarInt(true)
    val set = LongOpenHashSet(length)
    for (n in 0 until length) {
      set.add(input.readLong())
    }
    return set
  }
}

object Long2ObjectMapSerializer : Serializer<Long2ObjectMap<*>>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Long2ObjectMap<*>,
  ) {
    val valueType = kryo.generics.nextGenericClass()
    val isPolymorphic = valueType == null || !kryo.isFinal(valueType)
    val serializer = if (isPolymorphic) {
      null
    } else {
      kryo.getSerializer(valueType)
    }

    output.writeVarInt(obj.size, true)
    for ((key, value) in obj.long2ObjectEntrySet()) {
      output.writeVarLong(key, true)
      if (isPolymorphic) {
        kryo.writeClassAndObject(output, value)
      } else {
        kryo.writeObjectOrNull(output, value, serializer)
      }
    }

    kryo.generics.popGenericType()
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Long2ObjectMap<*>>,
  ): Long2ObjectMap<*> {
    val valueType = kryo.generics.nextGenericClass()
    val isPolymorphic = valueType == null || !kryo.isFinal(valueType)
    val serializer = if (isPolymorphic) {
      null
    } else {
      kryo.getSerializer(valueType)
    }

    val map = Long2ObjectLinkedOpenHashMap<Any?>()
    val length = input.readVarInt(true)
    for (n in 0 until length) {
      val key = input.readVarLong(true)
      val value = if (isPolymorphic) {
        kryo.readClassAndObject(input)
      } else {
        kryo.readObject(input, valueType, serializer)
      }
      map.put(key, value)
    }

    kryo.generics.popGenericType()
    return map
  }

}

object Object2LongMapSerializer : Serializer<Object2LongMap<*>>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Object2LongMap<*>,
  ) {
    val keyType = kryo.generics.nextGenericClass()
    val isPolymorphic = keyType == null || !kryo.isFinal(keyType)
    val serializer = if (isPolymorphic) {
      null
    } else {
      kryo.getSerializer(keyType)
    }

    output.writeVarInt(obj.size, true)
    for ((key, value) in obj.object2LongEntrySet()) {
      if (isPolymorphic) {
        kryo.writeClassAndObject(output, key)
      } else {
        kryo.writeObjectOrNull(output, key, serializer)
      }
      output.writeVarLong(value, true)
    }

    kryo.generics.popGenericType()
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Object2LongMap<*>>,
  ): Object2LongMap<*> {
    val keyType = kryo.generics.nextGenericClass()
    val isPolymorphic = keyType == null || !kryo.isFinal(keyType)
    val serializer = if (isPolymorphic) {
      null
    } else {
      kryo.getSerializer(keyType)
    }

    val map = Object2LongOpenHashMap<Any?>()
    val length = input.readVarInt(true)
    for (n in 0 until length) {
      val key = if (isPolymorphic) {
        kryo.readClassAndObject(input)
      } else {
        kryo.readObject(input, keyType, serializer)
      }
      val value = input.readVarLong(true)
      map.put(key, value)
    }

    kryo.generics.popGenericType()
    return map
  }
}
