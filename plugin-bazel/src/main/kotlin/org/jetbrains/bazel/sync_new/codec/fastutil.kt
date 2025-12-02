package org.jetbrains.bazel.sync_new.codec

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

object IntOpenHashSetCodec {
  fun encode(ctx: CodecContext, buffer: CodecBuffer, value: IntOpenHashSet) {
    buffer.writeVarInt(value.size)
    val iter = value.intIterator()
    while (iter.hasNext()) {
      buffer.writeVarInt(iter.nextInt())
    }
  }

  fun decode(ctx: CodecContext, buffer: CodecBuffer): IntOpenHashSet {
    val len = buffer.readVarInt()
    val set = IntOpenHashSet(len)
    for (n in 0 until len) {
      set.add(buffer.readVarInt())
    }
    return set
  }
}

object Int2ObjectOpenHashMapCodec {
  inline fun <T> encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: Int2ObjectOpenHashMap<T>,
    crossinline encode: (buffer: CodecBuffer, value: T) -> Unit,
  ) {
    buffer.writeVarInt(value.size)
    value.int2ObjectEntrySet().fastForEach {
      buffer.writeVarInt(it.intKey)
      encode(buffer, it.value)
    }
  }

  inline fun <T> decode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    crossinline decode: (buffer: CodecBuffer) -> T,
  ): Int2ObjectOpenHashMap<T> {
    val len = buffer.readVarInt()
    val map = Int2ObjectOpenHashMap<T>(len)
    for (n in 0 until len) {
      val key = buffer.readVarInt()
      map[key] = decode(buffer)
    }
    return map
  }
}

object Object2IntOpenHashMapCodec {
  inline fun <T> encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: Object2IntOpenHashMap<T>,
    crossinline encode: (buffer: CodecBuffer, value: T) -> Unit,
  ) {
    buffer.writeVarInt(value.size)
    value.object2IntEntrySet().fastForEach {
      encode(buffer, it.key)
      buffer.writeVarInt(it.intValue)
    }
  }

  inline fun <T> decode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    crossinline decode: (buffer: CodecBuffer) -> T,
  ): Object2IntOpenHashMap<T> {
    val len = buffer.readVarInt()
    val map = Object2IntOpenHashMap<T>(len)
    for (n in 0 until len) {
      val key = decode(buffer)
      map[key] = buffer.readVarInt()
    }
    return map
  }
}

object IntListCodec {
  fun encode(ctx: CodecContext, buffer: CodecBuffer, value: IntList) {
    buffer.writeVarInt(value.size)
    for (n in value.indices) {
      buffer.writeVarInt(value.getInt(n))
    }
  }

  fun decode(ctx: CodecContext, buffer: CodecBuffer): IntList {
    val len = buffer.readVarInt()
    val list = it.unimi.dsi.fastutil.ints.IntArrayList(len)
    for (n in 0 until len) {
      list.add(buffer.readVarInt())
    }
    return list
  }
}
