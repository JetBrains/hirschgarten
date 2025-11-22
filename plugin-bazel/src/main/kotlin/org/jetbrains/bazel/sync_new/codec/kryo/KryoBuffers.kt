package org.jetbrains.bazel.sync_new.codec.kryo

import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import org.jetbrains.bazel.sync_new.codec.ReadableOnlyCodecBuffer
import org.jetbrains.bazel.sync_new.codec.WritableOnlyCodecBuffer
import java.lang.invoke.MethodHandles
import java.nio.ByteBuffer

@JvmInline
value class KryoWriteCodecBuffer(private val output: Output) : WritableOnlyCodecBuffer {
  companion object {
    private val OUTPUT_REQUIRE_HANDLE = MethodHandles.privateLookupIn(Output::class.java, MethodHandles.lookup())
      .unreflect(Output::class.java.getDeclaredMethod("require", Int::class.java))
  }

  override val position: Int
    get() = output.position()
  override val size: Int
    get() = output.maxCapacity

  override fun reserve(size: Int) {
    OUTPUT_REQUIRE_HANDLE.invoke(output, size)
  }

  override fun writeVarInt(value: Int) {
    output.writeVarInt(value, true)
  }

  override fun writeVarLong(value: Long) {
    output.writeVarLong(value, true)
  }

  override fun writeInt8(value: Byte) {
    output.writeByte(value)
  }

  override fun writeInt32(value: Int) {
    output.writeInt(value, false)
  }

  override fun writeInt64(value: Long) {
    output.writeLong(value, false)
  }

  override fun writeBytes(bytes: ByteArray) {
    output.write(bytes)
  }

  override fun writeBuffer(buffer: ByteBuffer) {
    if (buffer.hasArray()) {
      output.writeBytes(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
    } else {
      val bytes = ByteArray(buffer.remaining())
      buffer.get(bytes)
      output.write(bytes)
    }
  }

}

@JvmInline
value class KryoReadCodecBuffer(private val input: Input) : ReadableOnlyCodecBuffer {
  override val position: Int
    get() = input.position()
  override val size: Int
    get() = input.limit()

  override fun readVarInt(): Int = input.readVarInt(true)

  override fun readVarLong(): Long = input.readVarLong(true)

  override fun readInt8(): Byte = input.readByte()

  override fun readInt32(): Int = input.readInt(false)

  override fun readInt64(): Long = input.readLong(false)

  override fun readBytes(bytes: ByteArray): Unit = input.readBytes(bytes)

  override fun readBuffer(size: Int): ByteBuffer {
    val buffer = ByteBuffer.allocate(size)
    input.readBytes(buffer.array(), 0, size)
    buffer.position(0)
    return buffer
  }


}
