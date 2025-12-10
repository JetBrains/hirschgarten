package org.jetbrains.bazel.sync_new.storage.mvstore

import org.h2.mvstore.DataUtils
import org.h2.mvstore.WriteBuffer
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.HasByteBuffer
import org.jetbrains.bazel.sync_new.codec.ReadableOnlyCodecBuffer
import org.jetbrains.bazel.sync_new.codec.WritableOnlyCodecBuffer
import java.lang.invoke.MethodHandles
import java.nio.ByteBuffer

@JvmInline
value class MVStoreWriteCodecBuffer(
  private val writeBuffer: WriteBuffer,
) : WritableOnlyCodecBuffer, HasByteBuffer {
  companion object {
    private val WRITE_BUFFER_GROW_HANDLE = MethodHandles.privateLookupIn(WriteBuffer::class.java, MethodHandles.lookup())
      .unreflect(WriteBuffer::class.java.getDeclaredMethod("grow", Int::class.java))
  }

  override val position: Int
    get() = writeBuffer.position()
  override val size: Int
    get() = writeBuffer.capacity()

  override fun reserve(size: Int) {
    WRITE_BUFFER_GROW_HANDLE.invokeExact(writeBuffer, size)
  }

  override fun flip() {
    buffer.flip()
  }

  override fun clear() {
    buffer.clear()
  }

  override fun writeVarInt(value: Int) {
    writeBuffer.putVarInt(value)
  }

  override fun writeVarLong(value: Long) {
    writeBuffer.putVarLong(value)
  }

  override fun writeInt8(value: Byte) {
    writeBuffer.put(value)
  }

  override fun writeInt32(value: Int) {
    writeBuffer.putInt(value)
  }

  override fun writeInt64(value: Long) {
    writeBuffer.putLong(value)
  }

  override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
    writeBuffer.put(bytes, offset, length)
  }

  override fun writeBuffer(buffer: ByteBuffer) {
    this.writeBuffer.put(buffer)
  }

  override val buffer: ByteBuffer
    get() = writeBuffer.buffer
}

@JvmInline
value class MVStoreReadCodecBuffer(
  override val buffer: ByteBuffer,
) : ReadableOnlyCodecBuffer, HasByteBuffer {
  override val position: Int
    get() = buffer.position()
  override val size: Int
    get() = buffer.capacity()

  override fun flip() {
    buffer.flip()
  }

  override fun clear() {
    buffer.clear()
  }

  override fun readVarInt(): Int = DataUtils.readVarInt(buffer)

  override fun readVarLong(): Long = DataUtils.readVarLong(buffer)

  override fun readInt8(): Byte = buffer.get()

  override fun readInt32(): Int = buffer.getInt()

  override fun readInt64(): Long = buffer.getLong()

  override fun readBytes(bytes: ByteArray, offset: Int, length: Int) {
    buffer.get(bytes, offset, length)
  }

  override fun readBuffer(size: Int): ByteBuffer {
    val slice = buffer.slice(buffer.position(), size)
    buffer.position(buffer.position() + size)
    return slice
  }
}
