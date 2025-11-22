package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.SerializerFactory
import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer
import com.esotericsoftware.kryo.kryo5.serializers.TaggedFieldSerializer

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tagged

class KryoObjectSerializerFactory(
  val taggedConfig: TaggedFieldSerializer.TaggedFieldSerializerConfig = TaggedFieldSerializer.TaggedFieldSerializerConfig(),
  val defaultConfig: FieldSerializer.FieldSerializerConfig = FieldSerializer.FieldSerializerConfig()
) : SerializerFactory.BaseSerializerFactory<FieldSerializer<*>>() {
  override fun newSerializer(
    kryo: Kryo?,
    type: Class<*>?,
  ): FieldSerializer<*>? {
    return if (type?.isAnnotationPresent(Tagged::class.java) == true) {
      TaggedFieldSerializer<Any>(kryo, type, taggedConfig)
    } else {
      FieldSerializer<Any>(kryo, type, defaultConfig)
    }
  }
}
