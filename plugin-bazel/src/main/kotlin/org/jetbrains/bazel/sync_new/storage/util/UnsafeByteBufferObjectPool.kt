package org.jetbrains.bazel.sync_new.storage.util

import java.util.concurrent.TimeUnit

internal object UnsafeByteBufferObjectPool {
  val pool = BoundedBlockingObjectPool(maxSize = 64) { UnsafeByteBufferCodecBuffer.allocateUnsafe(size = 1024 * 1024) }

  fun acquire(): UnsafeByteBufferCodecBuffer {
    val buffer = pool.acquire(100, TimeUnit.MILLISECONDS) ?: error("No buffers available")
    buffer.clear()
    return buffer
  }

  fun release(buffer: UnsafeByteBufferCodecBuffer) = pool.release(buffer)

  inline fun <T> use(crossinline block: (buffer: UnsafeByteBufferCodecBuffer) -> T): T {
    val buffer = acquire()
    try {
      return block(buffer)
    } finally {
      release(buffer)
    }
  }

  inline fun <T> use(crossinline block: (b1: UnsafeByteBufferCodecBuffer, b2: UnsafeByteBufferCodecBuffer) -> T) : T {
    val b1 = acquire()
    val b2 = acquire()
    try {
      return block(b1, b2)
    } finally {
      release(b1)
      release(b2)
    }
  }
}
