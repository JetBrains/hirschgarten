package org.jetbrains.bazel.sync_new.storage.util

import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PagedFileChannelCodecBuffer(val channel: FileChannel) : CodecBuffer, ByteBufferCompat {
  companion object {
    private const val PAGE_SIZE = 32L * 1024 * 1024 // 32mb
    private const val PAGE_SHIFT = 25 // 2^25 = 32mb
  }

  private var pages: Array<MappedByteBuffer> = emptyArray()
  private var _position: Int = 0
  private var _limit: Int = 0

  override val writable: Boolean = true
  override val readable: Boolean = true
  override var position: Int
    get() = _position
    set(value) { _position = value }
  override val size: Int
    get() = _limit

  override fun reserve(size: Int) {
    this.ensureMapped(_position, size)
  }

  override fun flip() {
    _position = 0
  }

  override fun clear() {
    _position = 0
    _limit = 0
  }

  override fun writeVarInt(value: Int) {
    this.reserve(5)
    ByteBufferUtils.writeVarInt(this, value)
  }

  override fun writeVarLong(value: Long) {
    this.reserve(10)
    ByteBufferUtils.writeVarLong(this, value)
  }

  override fun writeInt8(value: Byte) {
    this.reserve(1)
    put(value)
  }

  override fun writeInt32(value: Int) {
    this.reserve(4)
    ByteBufferUtils.writeInt(this, value)
  }

  override fun writeInt64(value: Long) {
    this.reserve(8)
    ByteBufferUtils.writeLong(this, value)
  }

  override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
    writeBuffer(ByteBuffer.wrap(bytes, offset, length))
  }

  override fun writeBuffer(buffer: ByteBuffer) {
    val size = buffer.remaining()
    writeBuffer(_position, buffer)
    _position += size
  }

  override fun readVarInt(): Int {
    ensureMapped(_position, 5)
    return ByteBufferUtils.readVarInt(this)
  }

  override fun readVarLong(): Long {
    ensureMapped(_position, 10)
    return ByteBufferUtils.readVarLong(this)
  }

  override fun readInt8(): Byte {
    ensureMapped(_position, 1)
    return get()
  }

  override fun readInt32(): Int {
    ensureMapped(_position, 4)
    return ByteBufferUtils.readInt(this)
  }

  override fun readInt64(): Long {
    ensureMapped(_position, 8)
    return ByteBufferUtils.readLong(this)
  }

  override fun readBytes(bytes: ByteArray, offset: Int, length: Int) {
    ensureMapped(_position, length)
    var remaining = length
    var currentOffset = offset

    while (remaining > 0) {
      val pageIndex = _position shr PAGE_SHIFT
      val pageOffset = _position and ((1 shl PAGE_SHIFT) - 1)
      val available = PAGE_SIZE.toInt() - pageOffset
      val toCopy = minOf(remaining, available)

      pages[pageIndex].position(pageOffset)
      pages[pageIndex].get(bytes, currentOffset, toCopy)

      _position += toCopy
      currentOffset += toCopy
      remaining -= toCopy
    }
  }

  override fun readBuffer(size: Int): ByteBuffer {
    ensureMapped(_position, size)
    val buffer = ByteBuffer.allocate(size)
    var remaining = size

    while (remaining > 0) {
      val pageIndex = _position shr PAGE_SHIFT
      val pageOffset = _position and ((1 shl PAGE_SHIFT) - 1)
      val available = PAGE_SIZE.toInt() - pageOffset
      val toCopy = minOf(remaining, available)

      pages[pageIndex].position(pageOffset)
      val oldLimit = pages[pageIndex].limit()
      pages[pageIndex].limit(pageOffset + toCopy)
      buffer.put(pages[pageIndex])
      pages[pageIndex].limit(oldLimit)

      _position += toCopy
      remaining -= toCopy
    }

    buffer.flip()
    return buffer
  }

  private fun ensureMapped(position: Int, size: Int) {
    val endPosition = position + size - 1
    val startPage = position shr PAGE_SHIFT
    val endPage = endPosition shr PAGE_SHIFT

    for (index in startPage..endPage) {
      while (index >= pages.size) {
        val pageIndex = pages.size
        val page = channel.map(
          /* mode = */ FileChannel.MapMode.READ_WRITE,
          /* position = */ pageIndex * PAGE_SIZE,
          /* size = */ PAGE_SIZE,
        )
        pages += page
      }
    }
  }

  override fun get(): Byte {
    val index = _position shr PAGE_SHIFT
    val offset = _position and ((1 shl PAGE_SHIFT) - 1)
    val result = pages[index].get(offset)
    _position++
    return result
  }

  override fun put(value: Byte) {
    val index = _position shr PAGE_SHIFT
    val offset = _position and ((1 shl PAGE_SHIFT) - 1)
    pages[index].put(offset, value)
    _position++
    if (_position > _limit) {
      _limit = _position
    }
  }

  fun writeBuffer(position: Int, buffer: ByteBuffer) {
    val startPosition = buffer.position()
    val length = buffer.remaining()
    ensureMapped(position, length)
    
    var currentPos = position
    var remaining = length

    while (remaining > 0) {
      val pageIndex = currentPos shr PAGE_SHIFT
      val pageOffset = currentPos and ((1 shl PAGE_SHIFT) - 1)
      val available = PAGE_SIZE.toInt() - pageOffset
      val toCopy = minOf(remaining, available)

      pages[pageIndex].position(pageOffset)
      val oldLimit = buffer.limit()
      buffer.limit(buffer.position() + toCopy)
      pages[pageIndex].put(buffer)
      buffer.limit(oldLimit)

      currentPos += toCopy
      remaining -= toCopy
    }

    buffer.position(startPosition)

    if (currentPos > _limit) {
      _limit = currentPos
    }
  }

  fun flush() {
    for (buffer in pages) {
      buffer.force()
    }
  }
}
