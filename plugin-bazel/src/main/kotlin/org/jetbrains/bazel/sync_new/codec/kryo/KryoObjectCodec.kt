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
import java.util.EnumSet


private val kryoThreadLocal = ThreadLocal.withInitial {
  val kryo = Kryo()
  kryo.classLoader = KryoObjectCodec::class.java.classLoader
  kryo.isRegistrationRequired = false
  kryo.setAutoReset(false)
  kryo.references = true
  //kryo.setCopyReferences(true)
  kryo.instantiatorStrategy = DefaultInstantiatorStrategy(StdInstantiatorStrategy())

  kryo.setDefaultSerializer(KryoCompositeSerializeFactory(
    factories = listOf(
      KryoSealedInterfaceSerializerFactory(),
      KryoEnumSerializerFactory(),
      KryoTaggedCompositeSerializeFactory()
    ),
    fallback = FieldSerializerFactory()
  ))

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
) : Codec<T> {
  companion object {
    private const val BUFFER_SIZE = 512
  }

  override fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: T,
  ) {
    try {
      if (buffer is HasByteBuffer && false) {
        val output = if (buffer.buffer.isDirect) {
          UnsafeByteBufferOutput(BUFFER_SIZE, Int.MAX_VALUE)
        } else {
          ByteBufferOutput(BUFFER_SIZE, Int.MAX_VALUE)
        }
        kryo.writeObject(output, value)
        output.flush()
        val byteBuffer = output.byteBuffer
        byteBuffer.flip()
        val length = byteBuffer.remaining()
        buffer.writeVarInt(length)
        buffer.writeBuffer(byteBuffer)
        output.close()
      } else {
        val output = Output(BUFFER_SIZE, Int.MAX_VALUE)
        kryo.writeObject(output, value)
        output.flush()
        buffer.writeVarInt(output.position())
        buffer.writeBytes(output.toBytes())
        output.close()
      }
    } finally {
      kryo.reset()
    }
  }

  override fun decode(ctx: CodecContext, buffer: CodecBuffer): T {
    try {
      val length = buffer.readVarInt()
      if (buffer is HasByteBuffer && false) {
        val byteBuffer = buffer.readBuffer(length)
        val input = if (byteBuffer.isDirect) {
          UnsafeByteBufferInput(byteBuffer)
        } else {
          ByteBufferInput(byteBuffer)
        }
        return kryo.readObject(input, type)
      } else {
        val bytes = ByteArray(length)
        buffer.readBytes(bytes)
        val input = Input(bytes)
        return kryo.readObject(input, type)
      }
    } finally {
      kryo.reset()
    }
  }
}

inline fun <reified T> CodecBuilder.ofKryo(): Codec<T> = KryoObjectCodec(T::class.java)

