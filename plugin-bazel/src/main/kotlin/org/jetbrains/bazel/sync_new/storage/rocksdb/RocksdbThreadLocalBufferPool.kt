package org.jetbrains.bazel.sync_new.storage.rocksdb

import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer

class RocksdbThreadLocalBufferPool(
  private val initialSize: Int = DEFAULT_BUFFER_SIZE,
) {
  companion object {
    const val BUFFER_SHRINK_DELAY: Long = 60_000L // 1m
    const val DEFAULT_BUFFER_SIZE: Int = 16 * 1024
    const val BUFFER_SHRINK_GAP: Int = 1024
  }

  data class UnsafeBuffer(
    val buffer: UnsafeByteBufferCodecBuffer,
    var lastShrinkTimestamp: Long,
  )

  val buffer: ThreadLocal<UnsafeBuffer> = ThreadLocal.withInitial {
    UnsafeBuffer(
      buffer = UnsafeByteBufferCodecBuffer.allocateUnsafe(initialSize),
      lastShrinkTimestamp = System.currentTimeMillis(),
    )
  }

  inline fun <T> use(crossinline op: (UnsafeByteBufferCodecBuffer) -> T): T {
    val buffer = buffer.get()
    buffer.buffer.clear()
    val value = op(buffer.buffer)
    if (System.currentTimeMillis() - buffer.lastShrinkTimestamp > BUFFER_SHRINK_DELAY) {
      buffer.buffer.shrinkToSize(threshold = BUFFER_SHRINK_GAP)
      buffer.lastShrinkTimestamp = System.currentTimeMillis()
    }
    return value
  }
}
