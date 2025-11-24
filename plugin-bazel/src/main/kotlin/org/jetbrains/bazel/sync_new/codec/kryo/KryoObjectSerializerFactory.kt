package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.SerializerFactory
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tagged

class KryoCompositeSerializeFactory(
  val factories: List<SerializerFactory<*>>,
  val fallback: SerializerFactory<*>
) : SerializerFactory<Serializer<*>> {
  override fun newSerializer(
    kryo: Kryo?,
    type: Class<*>?,
  ): Serializer<*>? {
    for (factory in factories) {
      if (factory.isSupported(type)) {
        return factory.newSerializer(kryo, type)
      }
    }
    return fallback.newSerializer(kryo, type)
  }

  override fun isSupported(type: Class<*>?): Boolean = true

}

class KryoTaggedCompositeSerializeFactory : SerializerFactory<TaggedFieldSerializer<*>> {
  override fun newSerializer(
    kryo: Kryo?,
    type: Class<*>?,
  ): TaggedFieldSerializer<*> {
    val config = TaggedFieldSerializer.TaggedFieldSerializerConfig()
    return TaggedFieldSerializer<Any>(kryo, type, config)
  }

  override fun isSupported(type: Class<*>): Boolean {
    return type.isAnnotationPresent(Tagged::class.java)
  }

}

class FieldSerializerFactory : SerializerFactory<FieldSerializer<*>> {
  override fun newSerializer(
    kryo: Kryo?,
    type: Class<*>?,
  ): FieldSerializer<*> {
    return FieldSerializer<Any>(kryo, type)
  }
  override fun isSupported(type: Class<*>): Boolean = true
}
