package org.jetbrains.bazel.sync_new.storage.mvstore

import org.h2.mvstore.DataUtils
import org.h2.mvstore.WriteBuffer
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import java.lang.invoke.MethodHandles
import java.nio.ByteBuffer

@JvmInline
value class MVStoreWriteCodecBuffer(
  private val buffer: WriteBuffer,
) : CodecBuffer {
  companion object {
    private val WRITE_BUFFER_GROW_HANDLE = MethodHandles.lookup()
      .unreflect(WriteBuffer::class.java.getDeclaredMethod("grow", Int::class.java))
  }

  override val writable: Boolean
    get() = true
  override val readable: Boolean
    get() = false
  override val position: Int
    get() = buffer.position()
  override val size: Int
    get() = buffer.capacity()

  override fun reserve(size: Int) {
    WRITE_BUFFER_GROW_HANDLE.invokeExact(buffer, size)
  }

  override fun writeVarInt(value: Int) {
    buffer.putVarInt(value)
  }

  override fun writeVarLong(value: Long) {
    buffer.putVarLong(value)
  }

  override fun writeInt32(value: Int) {
    buffer.putInt(value)
  }

  override fun writeInt64(value: Long) {
    buffer.putLong(value)
  }

  override fun writeBytes(bytes: ByteArray) {
    buffer.put(bytes)
  }

  override fun writeBuffer(buffer: ByteBuffer) {
    this.buffer.put(buffer)
  }

  override fun readVarInt(): Int = error("not supported")
  override fun readVarLong(): Long = error("not supported")
  override fun readInt32(): Int = error("not supported")
  override fun readInt64(): Long = error("not supported")
  override fun readBytes(bytes: ByteArray) = error("not supported")
  override fun readBuffer(size: Int): ByteBuffer = error("not supported")
}

@JvmInline
value class MVStoreReadCodecBuffer(
  private val buffer: ByteBuffer,
) : CodecBuffer {
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
    val result = ByteBuffer.allocate(size)
    buffer.get(result.array())
    result.position(0)
    return result
  }
}
