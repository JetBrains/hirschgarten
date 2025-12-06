package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.SerializerFactory.TaggedFieldSerializerFactory
import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput
import com.esotericsoftware.kryo.kryo5.io.ByteBufferOutput
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.kryo5.serializers.DefaultSerializers
import com.esotericsoftware.kryo.kryo5.unsafe.UnsafeByteBufferInput
import com.esotericsoftware.kryo.kryo5.unsafe.UnsafeByteBufferOutput
import com.esotericsoftware.kryo.kryo5.util.DefaultInstantiatorStrategy
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.codec.HasByteBuffer
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetTag
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.JvmModuleEntity
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.JvmResourceId
import java.util.EnumSet


private val kryoThreadLocal = ThreadLocal.withInitial {
  val kryo = Kryo()
  kryo.classLoader = KryoObjectCodec::class.java.classLoader
  kryo.isRegistrationRequired = false
  kryo.setAutoReset(true)
  kryo.references = true
  //kryo.setCopyReferences(true)
  kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

  // disallow d
  val sealedSerializer = KryoSealedInterfaceSerializerFactory(
    serializer =  KryoCompositeSerializeFactory(
      factories = listOf(
        KryoEnumSerializerFactory(),
        KryoTaggedCompositeSerializeFactory(),
      ),
      fallback = FieldSerializerFactory(),
    )
  )

  kryo.setDefaultSerializer(
    KryoCompositeSerializeFactory(
      factories = listOf(
        sealedSerializer,
        KryoEnumSerializerFactory(),
        KryoTaggedCompositeSerializeFactory(),
      ),
      fallback = FieldSerializerFactory(),
    ),
  )

  kryo.registerFastUtilSerializers()
  kryo.registerPrimitiveSerializers()
  kryo.registerGuavaSerializers()

  kryo.addDefaultSerializer(EnumSet::class.java, DefaultSerializers.EnumSetSerializer())
  kryo.register(BazelTargetTag::class.java)

  kryo
}

val kryo: Kryo
  get() = kryoThreadLocal.get()

// TODO: fix direct byte buffer writes
class KryoObjectCodec<T>(
  private val type: Class<T>,
  private val initialBufferSize: Int,
  private val useDirectBuffers: Boolean = false,
) : Codec<T> {

  override fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: T,
  ) {
    try {
      if (useDirectBuffers && buffer is HasByteBuffer) {
        encodeWithByteBuffer(ctx, buffer, value)
      } else {
        encodeWithByteArray(ctx, buffer, value)
      }
    } finally {
      kryo.reset()
    }
  }

  private fun encodeWithByteBuffer(
    ctx: CodecContext,
    buffer: HasByteBuffer,
    value: T,
  ) {
    val isDirect = buffer.buffer.isDirect
    val output = if (isDirect) {
      UnsafeByteBufferOutput(initialBufferSize, Int.MAX_VALUE)
    } else {
      ByteBufferOutput(initialBufferSize, Int.MAX_VALUE)
    }

    output.use { out ->
      kryo.writeObject(out, value)
      out.flush()

      val byteBuffer = out.byteBuffer
      val limit = byteBuffer.position()
      byteBuffer.flip()

      check(byteBuffer.limit() == limit) { "ByteBuffer flip failed: expected limit=$limit, got ${byteBuffer.limit()}" }

      val length = byteBuffer.remaining()
      buffer.writeVarInt(length)

      val readOnlySlice = byteBuffer.asReadOnlyBuffer()
      buffer.writeBuffer(readOnlySlice)
    }
  }

  private fun encodeWithByteArray(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: T,
  ) {
    Output(initialBufferSize, Int.MAX_VALUE).use { out ->
      kryo.writeObject(out, value)
      out.flush()

      val position = out.position()
      buffer.writeVarInt(position)

      val bytes = out.buffer
      buffer.writeBytes(bytes = bytes, offset = 0, length = position)
    }
  }

  override fun decode(ctx: CodecContext, buffer: CodecBuffer): T {
    try {
      val length = buffer.readVarInt()

      if (useDirectBuffers && buffer is HasByteBuffer) {
        return decodeWithByteBuffer(ctx, buffer, length)
      } else {
        return decodeWithByteArray(ctx, buffer, length)
      }
    } finally {
      kryo.reset()
    }
  }

  private fun decodeWithByteBuffer(
    ctx: CodecContext,
    buffer: HasByteBuffer,
    length: Int,
  ): T {
    val byteBuffer = buffer.readBuffer(length)
    if (byteBuffer.remaining() != length) {
      error("ByteBuffer has wrong remaining: expected=$length, actual=${byteBuffer.remaining()}")
    }

    val input = if (byteBuffer.isDirect) {
      UnsafeByteBufferInput(byteBuffer)
    } else {
      ByteBufferInput(byteBuffer)
    }

    return input.use { inp ->
      val result = kryo.readObject(inp, type)
      if (inp.available() > 0) {
        error("Not all data was read: ${inp.available()} bytes remaining")
      }
      result
    }
  }

  private fun decodeWithByteArray(
    ctx: CodecContext,
    buffer: CodecBuffer,
    length: Int,
  ): T {
    val bytes = ByteArray(length)
    buffer.readBytes(bytes)

    return Input(bytes).use { inp ->
      kryo.readObject(inp, type)
    }
  }
}

inline fun <reified T> CodecBuilder.ofKryo(initialBufferSize: Int = 64): Codec<T> = KryoObjectCodec(T::class.java, initialBufferSize)

