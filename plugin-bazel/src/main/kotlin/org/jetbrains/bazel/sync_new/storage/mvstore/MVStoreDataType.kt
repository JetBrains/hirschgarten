package org.jetbrains.bazel.sync_new.storage.mvstore

import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.DataType
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecContext
import java.nio.ByteBuffer

open class MVStoreDataType<T>(
  private val codec: Codec<T>,
  private val type: Class<T>,
) : DataType<T> {
  override fun compare(a: T, b: T): Int = throw UnsupportedOperationException()

  override fun binarySearch(key: T, storage: Any, size: Int, initialGuess: Int): Int = throw UnsupportedOperationException()

  override fun getMemory(obj: T): Int = codec.getSize(MVStoreCodecContext, obj)

  override fun isMemoryEstimationAllowed(): Boolean = false

  override fun write(buff: WriteBuffer, obj: T) {
    codec.encode(MVStoreCodecContext, MVStoreWriteCodecBuffer(buff), obj)
  }

  override fun write(buff: WriteBuffer, storage: Any, len: Int) {
    @Suppress("UNCHECKED_CAST")
    storage as Array<T>

    for (n in 0 until len) {
      write(buff, storage[n])
    }
  }

  override fun read(buff: ByteBuffer): T {
    return codec.decode(MVStoreCodecContext, MVStoreReadCodecBuffer(buff))
  }

  override fun read(buff: ByteBuffer, storage: Any, len: Int) {
    @Suppress("UNCHECKED_CAST")
    storage as Array<T>
    for (n in 0 until len) {
      read(buff)?.let { storage[n] = it }
    }
  }

  override fun createStorage(size: Int): Array<out T?>? {
    return java.lang.reflect.Array.newInstance(type, size) as Array<out T?>?
  }

}

class MVStoreOrderableDataType<T>(
  codec: Codec<T>,
  type: Class<T>,
  private val comparator: Comparator<T>,
) : MVStoreDataType<T>(codec, type) {
  override fun compare(a: T, b: T): Int = comparator.compare(a, b)

  override fun binarySearch(key: T, storage: Any, size: Int, initialGuess: Int): Int {
    @Suppress("UNCHECKED_CAST")
    storage as Array<T>
    var low = 0
    var hi = size - 1
    var x = initialGuess - 1
    if (x !in 0..hi) {
      x = hi ushr 1
    }
    while (low <= hi) {
      val v = storage[x]
      val cmp = comparator.compare(key, v)
      when {
        cmp > 0 -> low = x + 1
        cmp < 0 -> hi = x - 1
        else -> return x
      }
      x = (low + hi) ushr 1
    }
    return low.inv()
  }
}
