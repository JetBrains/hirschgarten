package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput
import com.esotericsoftware.kryo.kryo5.io.ByteBufferOutput
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.unsafe.UnsafeByteBufferInput
import com.esotericsoftware.kryo.kryo5.unsafe.UnsafeByteBufferOutput
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.codec.HasByteBuffer

private val kryoThreadLocal = ThreadLocal.withInitial {
  val kryo = Kryo()
  kryo.isRegistrationRequired = false

  kryo.registerFastUtilSerializers()
  kryo.registerPrimitiveSerializers()
  kryo.registerGuavaSerializers()
  kryo
}

val kryo: Kryo
  get() = kryoThreadLocal.get()

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
    if (buffer is HasByteBuffer) {
      val byteBuffer = buffer.buffer
      val output = if (byteBuffer.isDirect) {
        UnsafeByteBufferOutput(BUFFER_SIZE)
      } else {
        ByteBufferOutput(byteBuffer)
      }
      kryo.writeObject(output, value)
      buffer.writeVarInt(output.position())
      buffer.writeBuffer(output.byteBuffer)
    } else {
      val output = Output(BUFFER_SIZE)
      kryo.writeObject(output, value)
      buffer.writeVarInt(output.position())
      buffer.writeBytes(output.toBytes())
    }
  }

  override fun decode(ctx: CodecContext, buffer: CodecBuffer): T {
    val length = buffer.readVarInt()
    if (buffer is HasByteBuffer) {
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
  }
}

inline fun <reified T> CodecBuilder.ofKryo(): Codec<T> = KryoObjectCodec(T::class.java)

