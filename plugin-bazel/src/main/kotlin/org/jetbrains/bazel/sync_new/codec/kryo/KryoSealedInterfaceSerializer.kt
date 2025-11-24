package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.SerializerFactory
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SealedTagged

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SealedTag(val serialId: Int)

class KryoSealedInterfaceSerializer<T : Any>(
  private val type2Tag: Object2IntMap<KClass<out T>>,
  private val tag2Type: Int2ObjectMap<KClass<out T>>,
) : Serializer<T>() {
  override fun write(kryo: Kryo, output: Output, obj: T) {
    val tag = type2Tag.getOrDefault(obj::class, -1)
    if (tag < 0) {
      error("Unknown sealed type ${obj::class}")
    }
    output.writeVarInt(tag, true)
    kryo.writeObject(output, obj)
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out T>,
  ): T? {
    val tag = input.readVarInt(true)
    val type = tag2Type.getOrDefault(tag, null) ?: error("Unknown sealed tag $tag")
    return kryo.readObject(input, type.java)
  }

}

class KryoSealedInterfaceSerializerFactory(
) : SerializerFactory<KryoSealedInterfaceSerializer<*>> {

  override fun newSerializer(
    kryo: Kryo,
    type: Class<*>,
  ): KryoSealedInterfaceSerializer<*> {
    val type2Tag: Object2IntMap<KClass<*>> = Object2IntOpenHashMap()
    val tag2Type: Int2ObjectMap<KClass<*>> = Int2ObjectOpenHashMap()
    for (subClass in type.kotlin.sealedSubclasses) {
      val tag = subClass.findAnnotation<SealedTag>() ?: error("SealedTag annotation is missing for $subClass")
      type2Tag[subClass] = tag.serialId
      tag2Type[tag.serialId] = subClass
    }
    return KryoSealedInterfaceSerializer(type2Tag, tag2Type)
  }

  override fun isSupported(type: Class<*>): Boolean {
    return type.isSealed && type.isAnnotationPresent(SealedTagged::class.java)
  }
}
