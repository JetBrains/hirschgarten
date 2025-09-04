package org.jetbrains.bazel.utils.store.mvstore

import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.BasicDataType
import org.jetbrains.bazel.utils.store.codec.StoreCodec
import org.jetbrains.bazel.utils.store.codec.StoreCodecContext
import java.nio.ByteBuffer

abstract class MVStoreCodecDataType<T>(val codec: StoreCodec<T>) : BasicDataType<T>() {
  override fun getMemory(obj: T): Int = 0

  override fun isMemoryEstimationAllowed(): Boolean = true

  override fun write(buffer: WriteBuffer, data: T) {
    codec.encode(StoreCodecContext, data, MVStoreWriteCodecBuffer(buffer))
  }

  override fun read(buffer: ByteBuffer): T {
    return codec.decode(StoreCodecContext, MVStoreReadCodecBuffer(buffer))
  }

  override fun createStorage(size: Int): Array<T?> = arrayOfNulls(size)
}

class ValueMVStoreCodecDataType<T>(codec: StoreCodec<T>) : MVStoreCodecDataType<T>(codec) {
  override fun compare(one: T?, two: T?): Int = error("not supported")

  override fun binarySearch(keyObj: T?, storageObj: Any?, size: Int, initialGuess: Int): Int = error("not supported")
}

class ComparableMVStoreCodecDataType<T : Comparable<T>>(codec: StoreCodec<T>) : MVStoreCodecDataType<T>(codec) {
  override fun compare(one: T?, two: T?): Int {
    if (one == null && two == null) {
      return 0
    }
    if (one == null) {
      return -1
    }
    if (two == null) {
      return 1
    }
    return one.compareTo(two)
  }

  override fun binarySearch(keyObj: T?, storageObj: Any?, size: Int, initialGuess: Int): Int {
    var n = initialGuess
    while (n > 0) {
      val m = n / 2 - 1
      val mid = storageObj?.let { it as Array<T> }?.get(m)
      if (mid == null) {
        return if (keyObj == null) m else -m - 1
      }
      val cmp = compare(keyObj, mid)
      if (cmp < 0) {
        n = m
      }
    }
    return n.inv() - 1
  }
}
