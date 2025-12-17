package org.jetbrains.bazel.sync_new.storage.util

import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.HasByteBuffer
import java.nio.Buffer
import java.nio.ByteBuffer
import kotlin.math.max

class UnsafeByteBufferCodecBuffer(override var buffer: ByteBuffer) : CodecBuffer, HasByteBuffer {

  companion object {
    const val MAX_BUFFER_LENGTH: Int = Int.MAX_VALUE - 8
    const val DEFAULT_BUFFER_SIZE: Int = 256

    // TODO: can I use it just like that inside JBR?
    private val UNSAFE = UnsafeUtil.unsafe
    private val ADDRESS_OFFSET = UNSAFE.objectFieldOffset(Buffer::class.java.getDeclaredField("address"))

    fun allocateUnsafe(size: Int = DEFAULT_BUFFER_SIZE) =
      UnsafeByteBufferCodecBuffer(ByteBuffer.allocateDirect(size))

    fun allocateHeap(size: Int = DEFAULT_BUFFER_SIZE) =
      UnsafeByteBufferCodecBuffer(ByteBuffer.allocate(size))

    fun from(array: ByteArray) = UnsafeByteBufferCodecBuffer(ByteBuffer.wrap(array))
  }

  // allow both direct and heap ByteBuffer

  override val writable: Boolean = true
  override val readable: Boolean = true
  override val position: Int
    get() = buffer.position()
  override val size: Int
    get() = buffer.limit()

  override fun reserve(size: Int) {
    val required = buffer.position() + size
    if (required <= buffer.capacity()) {
      return
    }

    val newSize = calculateNewSize(buffer.capacity(), required)
    val newBuffer = if (buffer.isDirect) {
      ByteBuffer.allocateDirect(newSize)
    } else {
      ByteBuffer.allocate(newSize)
    }

    val oldPosition = buffer.position()
    buffer.position(0)
    buffer.limit(oldPosition)

    if (buffer.isDirect) {
      val oldAddress = UNSAFE.getLong(buffer, ADDRESS_OFFSET)
      val newAddress = UNSAFE.getLong(newBuffer, ADDRESS_OFFSET)

      UNSAFE.copyMemory(
        /* srcBase = */     null,
        /* srcOffset = */   oldAddress,
        /* destBase = */    null,
        /* destOffset = */  newAddress,
        /* bytes = */       oldPosition.toLong(),
      )
    } else {
      newBuffer.put(buffer)
    }

    newBuffer.position(oldPosition)
    buffer = newBuffer
  }

  override fun flip() {
    buffer.flip()
  }

  override fun clear() {
    buffer.clear()
  }

  override fun writeVarInt(value: Int) {
    reserve(5)
    ByteBufferBufferUtils.writeVarInt(buffer, value)
  }

  override fun writeVarLong(value: Long) {
    reserve(10)
    ByteBufferBufferUtils.writeVarLong(buffer, value)
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

  override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
    reserve(length)
    buffer.put(bytes, offset, length)
  }

  override fun writeBuffer(buffer: ByteBuffer) {
    reserve(buffer.remaining())
    this.buffer.put(buffer)
  }

  override fun readVarInt(): Int = ByteBufferBufferUtils.readVarInt(buffer)

  override fun readVarLong(): Long = ByteBufferBufferUtils.readVarLong(buffer)

  override fun readInt8(): Byte = buffer.get()

  override fun readInt32(): Int = buffer.getInt()

  override fun readInt64(): Long = buffer.getLong()

  override fun readBytes(bytes: ByteArray, offset: Int, length: Int) {
    buffer.get(bytes, offset, length)
  }

  override fun readBuffer(size: Int): ByteBuffer {
    val result = if (buffer.isDirect) {
      val newBuffer = ByteBuffer.allocateDirect(size)
      val srcAddress = UNSAFE.getLong(buffer, ADDRESS_OFFSET) + buffer.position()
      val destAddress = UNSAFE.getLong(newBuffer, ADDRESS_OFFSET)

      UNSAFE.copyMemory(
        /* srcBase = */     null,
        /* srcOffset = */   srcAddress,
        /* destBase = */    null,
        /* destOffset = */  destAddress,
        /* bytes = */       size.toLong(),
      )
      newBuffer.position(size)
      newBuffer.flip()
      newBuffer
    } else {
      val newBuffer = ByteBuffer.allocate(size)
      val oldLimit = buffer.limit()
      val oldPosition = buffer.position()
      buffer.limit(oldPosition + size)
      newBuffer.put(buffer) // advances position by `size`
      buffer.limit(oldLimit)
      newBuffer.flip()
      newBuffer
    }

    // manually advance only for direct buffers; heap buffers were advanced by put()
    if (buffer.isDirect) {
      buffer.position(buffer.position() + size)
    }

    return result
  }

  fun reset() {
    buffer.clear()
  }

  fun shrinkToSize(threshold: Int) {
    if (buffer.capacity() <= threshold) {
      return
    }

    val usedSize = buffer.position()
    val newBuffer = if (buffer.isDirect) {
      ByteBuffer.allocateDirect(usedSize)
    } else {
      ByteBuffer.allocate(usedSize)
    }

    val oldPosition = buffer.position()
    buffer.position(0)
    buffer.limit(usedSize)

    if (buffer.isDirect) {
      val srcAddress = UNSAFE.getLong(buffer, ADDRESS_OFFSET)
      val destAddress = UNSAFE.getLong(newBuffer, ADDRESS_OFFSET)

      UNSAFE.copyMemory(
        /* srcBase = */     null,
        /* srcOffset = */   srcAddress,
        /* destBase = */    null,
        /* destOffset = */  destAddress,
        /* bytes = */       usedSize.toLong(),
      )
    } else {
      newBuffer.put(buffer)
    }

    newBuffer.position(oldPosition)
    buffer = newBuffer
  }

  private fun calculateNewSize(current: Int, required: Int): Int {
    var value = max(current, 2)
    while (value < required) {
      value = value + (value shl 1)
      if (value > MAX_BUFFER_LENGTH) {
        value = MAX_BUFFER_LENGTH
      }
    }
    return value
  }

  fun resizeTo(required: Int) {
    if (buffer.capacity() >= required) {
      buffer.clear()
      return
    }
    val newSize = calculateNewSize(buffer.capacity(), required)
    buffer = if (buffer.isDirect) {
      ByteBuffer.allocateDirect(newSize)
    } else {
      ByteBuffer.allocate(newSize)
    }
  }
}
