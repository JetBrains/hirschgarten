package org.jetbrains.bazel.sync_new.lang.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.intellij.openapi.components.service
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import org.jetbrains.bazel.sync_new.lang.SyncLanguageService
import org.jetbrains.bazel.sync_new.lang.SyncTargetData

class SyncTargetDataSerializer : Serializer<Long2ObjectMap<SyncTargetData>>() {

  // cache service
  private val syncLanguageService = service<SyncLanguageService>()

  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Long2ObjectMap<SyncTargetData>,
  ) {
    output.writeVarInt(obj.size, true)
    for ((tag, data) in obj.long2ObjectEntrySet()) {
      output.writeLong(tag, false)
      kryo.writeObject(output, data)
    }
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Long2ObjectMap<SyncTargetData>>,
  ): Long2ObjectMap<SyncTargetData> {
    val size = input.readVarInt(true)
    val map = Long2ObjectOpenHashMap<SyncTargetData>(size)
    for (n in 0 until size) {
      val tag = input.readLong(false)
      val type = syncLanguageService.getTypeByTag(tag) ?: error("Unknown tag $tag")
      val data = kryo.readObject(input, type) as SyncTargetData
      map[tag] = data
    }

    return map
  }
}
