package org.jetbrains.bazel.sync_new.storage.rocksdb

import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import sun.misc.Unsafe
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.math.max

class UnsafeRocksdbCodecBuffer(var buffer: ByteBuffer) : CodecBuffer {

  companion object {
    const val MAX_BUFFER_LENGTH: Int = Int.MAX_VALUE - 8
    const val DEFAULT_BUFFER_SIZE: Int = 256

    // TODO: can I use it just like that inside JBR?
    private val UNSAFE = Unsafe.getUnsafe()
    private val ADDRESS_OFFSET = UNSAFE.objectFieldOffset(Buffer::class.java.getDeclaredField("address"))

    fun allocate(size: Int = DEFAULT_BUFFER_SIZE) = UnsafeRocksdbCodecBuffer(ByteBuffer.allocateDirect(size))
  }

  init {
    if (!buffer.isDirect) {
      error("UnsafeRocksdbCodecBuffer requires direct ByteBuffer")
    }
  }

  override val writable: Boolean = true
  override val readable: Boolean = true
  override val position: Int = buffer.position()
  override val size: Int = buffer.capacity()

  override fun reserve(size: Int) {
    val required = buffer.position() + size
    if (required < buffer.capacity()) {
      return
    }
    val newSize = calculateNewSize(buffer.capacity(), required)
    val newBuffer = ByteBuffer.allocateDirect(newSize)

    UNSAFE.copyMemory(
      /* srcBase = */     buffer,
      /* srcOffset = */   ADDRESS_OFFSET,
      /* destBase = */    newBuffer,
      /* destOffset = */  ADDRESS_OFFSET,
      /* bytes = */       buffer.position().toLong(),
    )

    newBuffer.position(buffer.position())
    buffer = newBuffer
  }

  override fun writeVarInt(value: Int) {
    reserve(5)
    RocksdbBufferUtils.writeVarInt(buffer, value)
  }

  override fun writeVarLong(value: Long) {
    reserve(10)
    RocksdbBufferUtils.writeVarLong(buffer, value)
  }

  override fun writeInt8(value: Byte) {
    reserve(1)
    buffer.put(value)
  }

  override fun writeInt32(value: Int) {
    reserve(4)
    buffer.putInt(value)
  }

  override fun writeInt64(value: Long) {
    reserve(8)
    buffer.putLong(value)
  }

  override fun writeBytes(bytes: ByteArray) {
    reserve(bytes.size)
    buffer.put(bytes)
  }

  override fun writeBuffer(buffer: ByteBuffer) {
    reserve(buffer.remaining())
    this.buffer.put(buffer)
  }

  override fun readVarInt(): Int = RocksdbBufferUtils.readVarInt(buffer)

  override fun readVarLong(): Long = RocksdbBufferUtils.readVarLong(buffer)

  override fun readInt8(): Byte = buffer.get()

  override fun readInt32(): Int = buffer.getInt()

  override fun readInt64(): Long = buffer.getLong()

  override fun readBytes(bytes: ByteArray) {
    buffer.get(bytes)
  }

  override fun readBuffer(size: Int): ByteBuffer {
    val result = if (buffer.isDirect) {
      val newBuffer = ByteBuffer.allocateDirect(size)
      UNSAFE.copyMemory(
        /* srcBase = */     buffer,
        /* srcOffset = */   ADDRESS_OFFSET + buffer.position(),
        /* destBase = */    newBuffer,
        /* destOffset = */  ADDRESS_OFFSET,
        /* bytes = */       size.toLong(),
      )
      newBuffer.position(size)
      newBuffer.flip()
      newBuffer
    } else {
      val newBuffer = ByteBuffer.allocate(size)
      val oldLimit = buffer.limit()
      buffer.limit(buffer.position() + size)
      newBuffer.put(buffer)
      buffer.limit(oldLimit)
      newBuffer.flip()
      newBuffer
    }
    buffer.position(buffer.position() + size)
    return result
  }


  fun reset() {
    buffer.position(0)
  }

  fun shrinkToSize(threshold: Int) {
    if (buffer.capacity() > threshold) {
      val usedSize = buffer.position()
      val newBuffer = ByteBuffer.allocateDirect(usedSize)

      val oldPosition = buffer.position()
      buffer.position(0)
      buffer.limit(usedSize)

      UNSAFE.copyMemory(
        /* srcBase = */     buffer,
        /* srcOffset = */   ADDRESS_OFFSET,
        /* destBase = */    newBuffer,
        /* destOffset = */  ADDRESS_OFFSET,
        /* bytes = */       usedSize.toLong(),
      )

      newBuffer.position(oldPosition)
      buffer = newBuffer
    }
  }

  fun calculateNewSize(current: Int, required: Int): Int {
    var value = max(current, 2)
    while (value < required) {
      value = value + value shl 1
      if (value > MAX_BUFFER_LENGTH) {
        value = MAX_BUFFER_LENGTH
      }
    }
    return value
  }
}
