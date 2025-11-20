package org.jetbrains.bazel.sync_new.storage.mvstore

import org.h2.mvstore.DataUtils
import org.h2.mvstore.WriteBuffer
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.HasByteBuffer
import java.lang.invoke.MethodHandles
import java.nio.ByteBuffer

@JvmInline
value class MVStoreWriteCodecBuffer(
  private val writeBuffer: WriteBuffer,
) : CodecBuffer, HasByteBuffer {
  companion object {
    private val WRITE_BUFFER_GROW_HANDLE = MethodHandles.lookup()
      .unreflect(WriteBuffer::class.java.getDeclaredMethod("grow", Int::class.java))
  }

  override val writable: Boolean
    get() = true
  override val readable: Boolean
    get() = false
  override val position: Int
    get() = writeBuffer.position()
  override val size: Int
    get() = writeBuffer.capacity()

  override fun reserve(size: Int) {
    WRITE_BUFFER_GROW_HANDLE.invokeExact(writeBuffer, size)
  }

  override fun writeVarInt(value: Int) {
    writeBuffer.putVarInt(value)
  }

  override fun writeVarLong(value: Long) {
    writeBuffer.putVarLong(value)
  }

  override fun writeInt32(value: Int) {
    writeBuffer.putInt(value)
  }

  override fun writeInt64(value: Long) {
    writeBuffer.putLong(value)
  }

  override fun writeBytes(bytes: ByteArray) {
    writeBuffer.put(bytes)
  }

  override fun writeBuffer(buffer: ByteBuffer) {
    this.writeBuffer.put(buffer)
  }

  override fun readVarInt(): Int = error("not supported")
  override fun readVarLong(): Long = error("not supported")
  override fun readInt32(): Int = error("not supported")
  override fun readInt64(): Long = error("not supported")
  override fun readBytes(bytes: ByteArray) = error("not supported")
  override fun readBuffer(size: Int): ByteBuffer = error("not supported")
  override val buffer: ByteBuffer
    get() = writeBuffer.buffer
}

@JvmInline
value class MVStoreReadCodecBuffer(
  override val buffer: ByteBuffer,
) : CodecBuffer, HasByteBuffer {
  override val writable: Boolean
    get() = false
  override val readable: Boolean
    get() = true
  override val position: Int
    get() = buffer.position()
  override val size: Int
    get() = buffer.capacity()

  override fun reserve(size: Int) = error("not supported")
  override fun writeVarInt(value: Int) = error("not supported")
  override fun writeVarLong(value: Long) = error("not supported")
  override fun writeInt32(value: Int) = error("not supported")
  override fun writeInt64(value: Long) = error("not supported")
  override fun writeBytes(bytes: ByteArray) = error("not supported")
  override fun writeBuffer(buffer: ByteBuffer) = error("not supported")

  override fun readVarInt(): Int = DataUtils.readVarInt(buffer)

  override fun readVarLong(): Long = DataUtils.readVarLong(buffer)

  override fun readInt32(): Int = buffer.getInt()

  override fun readInt64(): Long = buffer.getLong()

  override fun readBytes(bytes: ByteArray) {
    buffer.get(bytes)
  }

  override fun readBuffer(size: Int): ByteBuffer {
    return buffer.slice(buffer.position(), size)
  }
}
