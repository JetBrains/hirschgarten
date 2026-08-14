package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.BasicDataType
import java.nio.ByteBuffer

// used only for unopened MVMap pruning
internal object UnknownDataType : BasicDataType<ByteArray>() {
  override fun isMemoryEstimationAllowed(): Boolean = false

  override fun getMemory(obj: ByteArray): Int = obj.size

  override fun createStorage(size: Int): Array<ByteArray?> = arrayOfNulls(size)

  override fun write(buff: WriteBuffer, obj: ByteArray) {
    buff.put(obj)
  }

  override fun read(buff: ByteBuffer): ByteArray {
    val data = ByteArray(buff.remaining())
    buff.get(data)
    return data
  }
}
