package org.jetbrains.bazel.target

import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.DataType
import java.nio.ByteBuffer

internal inline fun <reified T : Any> createAnyValueDataType(
  crossinline writer: (buffer: WriteBuffer, item: T) -> Unit,
  crossinline reader: (buffer: ByteBuffer) -> T,
): DataType<T> {
  val emptyArray = arrayOfNulls<T>(0)
  return object : DataType<T> {
    override fun getMemory(obj: T): Int = 0

    override fun isMemoryEstimationAllowed(): Boolean = true

    override fun write(buffer: WriteBuffer, data: T): Unit = throw UnsupportedOperationException()

    override fun write(
      buffer: WriteBuffer,
      storage: Any,
      len: Int,
    ) {
      @Suppress("UNCHECKED_CAST")
      storage as Array<T>
      for (item in storage) {
        writer(buffer, item)
      }
    }

    override fun read(buffer: ByteBuffer): T = throw UnsupportedOperationException()

    override fun read(
      buffer: ByteBuffer,
      storage: Any,
      size: Int,
    ) {
      @Suppress("UNCHECKED_CAST")
      storage as Array<T>
      for (i in 0..<size) {
        storage[i] = reader(buffer)
      }
    }

    override fun createStorage(size: Int): Array<T?> = if (size == 0) emptyArray else arrayOfNulls(size)

    override fun compare(one: T, two: T): Int = throw UnsupportedOperationException()

    override fun binarySearch(
      keyObj: T,
      storageObj: Any,
      size: Int,
      initialGuess: Int,
    ): Int = throw UnsupportedOperationException()
  }
}
