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
import kotlin.reflect.full.hasAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SealedTagged

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SealedTag(val serialId: Int)

class KryoSealedInterfaceSerializer<T : Any>(
  private val type2Tag: Object2IntMap<KClass<out T>>,
  private val tag2Type: Int2ObjectMap<KClass<out T>>,
  private val tag2Serializer: Int2ObjectMap<Serializer<out T>>
) : Serializer<T>() {
  override fun write(kryo: Kryo, output: Output, obj: T) {
    val tag = type2Tag.getOrDefault(obj::class, -1)
    if (tag < 0) {
      error("Unknown sealed type ${obj::class}")
    }
    output.writeVarInt(tag, true)
    (tag2Serializer[tag] as Serializer<T>).write(kryo, output, obj)
    //kryo.writeObject(output, obj, tag2Serializer[tag])
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out T>,
  ): T? {
    val tag = input.readVarInt(true)
    val type = tag2Type.getOrDefault(tag, null) ?: error("Unknown sealed tag $tag")
    return (tag2Serializer[tag] as Serializer<T>).read(kryo, input, type.java)
    //return kryo.readObject(input, type.java, tag2Serializer[tag])
  }

}

class KryoSealedInterfaceSerializerFactory(
  private val serializer: SerializerFactory<*>
) : SerializerFactory<KryoSealedInterfaceSerializer<*>> {

  // kryo instances are thread safe
  val sealed2Serializer: MutableMap<Class<*>, KryoSealedInterfaceSerializer<*>> = mutableMapOf()

  override fun newSerializer(
    kryo: Kryo,
    type: Class<*>,
  ): KryoSealedInterfaceSerializer<*> {
    val sealed = findSealedInterfaces(type.kotlin) ?: error("illegal state")
    return sealed2Serializer.getOrPut(sealed.java) {
      val type2Tag: Object2IntMap<KClass<*>> = Object2IntOpenHashMap()
      val tag2Type: Int2ObjectMap<KClass<*>> = Int2ObjectOpenHashMap()
      val tag2Serializer: Int2ObjectMap<Serializer<*>> = Int2ObjectOpenHashMap()
      for (subClass in findAllSealedImpls(sealed)) {
        val tag = subClass.findAnnotation<SealedTag>() ?: error("SealedTag annotation is missing for $subClass")
        type2Tag[subClass] = tag.serialId
        tag2Type[tag.serialId] = subClass
        tag2Serializer[tag.serialId] = serializer.newSerializer(kryo, subClass.java)
      }
      KryoSealedInterfaceSerializer(type2Tag, tag2Type, tag2Serializer)
    }
  }

  override fun isSupported(type: Class<*>): Boolean {
    return isSealedInterface(type.kotlin) || findSealedInterfaces(type.kotlin) != null
  }

  private fun findSealedInterfaces(type: KClass<*>): KClass<*>? {
    val queue = ArrayDeque<KClass<*>>()
    queue.add(type)
    while (true) {
      val superType = queue.removeFirstOrNull() ?: break
      if (isSealedInterface(superType)) {
        return superType
      }
      for (superType in superType.supertypes) {
        val classifier = superType.classifier
        if (classifier is KClass<*>) {
          queue.add(classifier)
        }
      }
    }
    return null
  }

  private fun findAllSealedImpls(type: KClass<*>): List<KClass<*>> {
    val result = mutableListOf<KClass<*>>()
    val queue = ArrayDeque<KClass<*>>()
    queue.add(type)
    while (true) {
      val superType = queue.removeFirstOrNull() ?: break
      if (superType.hasAnnotation<SealedTag>()) {
        result.add(superType)
      }
      for (subClass in superType.sealedSubclasses) {
        queue.add(subClass)
      }
    }
    return result
  }

  private fun isSealedInterface(type: KClass<*>): Boolean = type.isSealed
    && type.hasAnnotation<SealedTagged>()
}
