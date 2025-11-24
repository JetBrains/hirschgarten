package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.SerializerFactory
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnumTagged

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnumTag(val serialId: Int)

class KryoEnumSerializer<T : Enum<T>>(
  private val ordinal2Tag: Int2IntMap,
  private val tag2Ordinal: Int2IntMap,
  private val constantsCache: Array<T>,
) : Serializer<Enum<T>>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: Enum<T>,
  ) {
    val tag = ordinal2Tag.getOrDefault(obj.ordinal, -1)
    if (tag < 0) {
      error("Unknown enum tag for ${obj.javaClass.name} ${obj.name}")
    }
    output.writeVarInt(tag, true)
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out Enum<T>>,
  ): Enum<T> {
    val tag = input.readVarInt(true)
    val ordinal = tag2Ordinal.getOrDefault(tag, -1)
    if (ordinal < 0) {
      error("Unknown enum tag $tag")
    }
    return constantsCache[ordinal]
  }
}

class KryoEnumSerializerFactory<T : Enum<T>> : SerializerFactory<KryoEnumSerializer<T>> {
  override fun newSerializer(
    kryo: Kryo,
    type: Class<*>,
  ): KryoEnumSerializer<T> {
    val allTagged = type.declaredFields.filter { it.isEnumConstant }
      .all { it.isAnnotationPresent(EnumTag::class.java) }
    if (!allTagged) {
      error("All enum constants must be tagged with @EnumTag")
    }
    val ordinal2Tag = createInt2IntMap(type.enumConstants)
    val tag2Ordinal = createInt2IntMap(type.enumConstants)
    for (field in type.declaredFields) {
      if (!field.isEnumConstant) {
        continue
      }
      val tag = field.getAnnotation(EnumTag::class.java)
      val value = field.get(null) as T
      ordinal2Tag[value.ordinal] = tag.serialId
      tag2Ordinal[tag.serialId] = value.ordinal
    }
    return KryoEnumSerializer(
      ordinal2Tag,
      tag2Ordinal,
      type.enumConstants as Array<T>,
    )
  }

  override fun isSupported(type: Class<*>): Boolean {
    return type.isEnum && type.isAnnotationPresent(EnumTagged::class.java)
  }

  private fun createInt2IntMap(constants: Array<*>): Int2IntMap =
    if (constants.size < 6) {
      Int2IntArrayMap()
    } else {
      Int2IntOpenHashMap()
    }


}
