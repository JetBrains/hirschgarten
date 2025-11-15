package org.jetbrains.bazel.sync_new.graph.impl

import com.dynatrace.hash4j.hashing.HashValue128
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jetbrains.bazel.sync_new.codec.versionedCodecOf
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

class Hash2IdWire(
  val map: Object2LongMap<HashValue128> = Object2LongOpenHashMap(),
) {
  companion object {
    internal const val CODEC_VERSION = 1
    internal val codec = versionedCodecOf(
      version = CODEC_VERSION,
      encode = { _, buf, value ->
        buf.writeVarInt(value.map.size)
        for ((hash, id) in value.map.object2LongEntrySet()) {
          buf.writeVarLong(hash.mostSignificantBits)
          buf.writeVarLong(hash.leastSignificantBits)
          buf.writeVarLong(id)
        }
      },
      decode = { _, buf, version ->
        check(version == CODEC_VERSION)
        val mapLen = buf.readVarInt()
        val map = Object2LongOpenHashMap<HashValue128>(mapLen)
        for (a in 0 until mapLen) {
          val hashHi = buf.readVarLong()
          val hashLo = buf.readVarLong()
          val id = buf.readVarLong()
          map.put(HashValue128(hashHi, hashLo), id)
        }
        return@versionedCodecOf Hash2IdWire(map = map)
      },
    )
  }
}

data class Id2ListIdWire(
  val map: Long2ObjectMap<LongList> = Long2ObjectOpenHashMap(),
) {
  companion object {
    internal const val CODEC_VERSION = 1
    internal val codec = versionedCodecOf(
      version = CODEC_VERSION,
      encode = { _, buf, value ->
        buf.writeVarInt(value.map.size)
        for ((vertexId, list) in value.map) {
          buf.writeVarLong(vertexId)
          buf.writeVarInt(list.size)
          for (n in list.indices) {
            buf.writeVarLong(list.getLong(n))
          }
        }
      },
      decode = { _, buf, version ->
        check(version == CODEC_VERSION)
        val mapLen = buf.readVarInt()
        val map = Long2ObjectOpenHashMap<LongList>(mapLen)
        for (a in 0 until mapLen) {
          val fromVertexId = buf.readVarLong()
          val setLen = buf.readVarInt()
          val list = LongArrayList(setLen)
          for (b in 0 until setLen) {
            val toVertexId = buf.readVarLong()
            list.add(toVertexId)
          }
          map.put(fromVertexId, list)
        }
        return@versionedCodecOf Id2ListIdWire(map = map)
      },
    )
  }

  fun add(fromId: Long, toId: Long) {
    map.computeIfAbsent(fromId) { LongArrayList() }.add(toId)
  }

  fun remove(fromId: Long, toId: Long) {
    map.get(fromId)?.removeIf { it == toId }
  }

  fun get(fromId: Long): LongList? = map.get(fromId)
}

data class Hash2ListIdWire(
  val map: Object2ObjectMap<HashValue128, LongList> = Object2ObjectOpenHashMap(),
) {
  companion object {
    internal const val CODEC_VERSION = 1
    internal val codec = versionedCodecOf(
      version = CODEC_VERSION,
      encode = { _, buf, value ->
        buf.writeVarInt(value.map.size)
        for ((hash, list) in value.map) {
          buf.writeVarLong(hash.mostSignificantBits)
          buf.writeVarLong(hash.leastSignificantBits)
          buf.writeVarInt(list.size)
          for (n in list.indices) {
            buf.writeVarLong(list.getLong(n))
          }
        }
      },
      decode = { _, buf, version ->
        check(version == CODEC_VERSION)
        val mapLen = buf.readVarInt()
        val map = Object2ObjectOpenHashMap<HashValue128, LongList>(mapLen)
        for (a in 0 until mapLen) {
          val hashHi = buf.readVarLong()
          val hashLo = buf.readVarLong()
          val setLen = buf.readVarInt()
          val list = LongArrayList(setLen)
          for (b in 0 until setLen) {
            val toVertexId = buf.readVarLong()
            list.add(toVertexId)
          }
          map[HashValue128(hashHi, hashLo)] = list
        }
        return@versionedCodecOf Hash2ListIdWire(map = map)
      },
    )
  }
}
