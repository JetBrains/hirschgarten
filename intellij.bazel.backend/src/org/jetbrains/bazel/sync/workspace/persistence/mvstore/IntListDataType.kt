package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import it.unimi.dsi.fastutil.ints.IntArrayList
import org.h2.mvstore.DataUtils
import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.BasicDataType
import java.nio.ByteBuffer

internal object IntListDataType : BasicDataType<IntArrayList>() {
  override fun getMemory(obj: IntArrayList): Int = obj.size * Int.SIZE_BYTES

  override fun write(buff: WriteBuffer, obj: IntArrayList) {
    buff.putVarInt(obj.size)

    // `grow` inside `WriteBuffer` is private, so just manually insert elements
    // not ideal but lists using this codec usually doesn't have more than ~20 elements
    for (n in obj.indices) {
      buff.putInt(obj.getInt(n))
    }
  }

  override fun read(buff: ByteBuffer): IntArrayList {
    val size = DataUtils.readVarInt(buff)
    val array = IntArray(size)
    buff.asIntBuffer().get(array)
    buff.position(buff.position() + size * Int.SIZE_BYTES)
    return IntArrayList(array)
  }

  override fun createStorage(size: Int): Array<out IntArrayList?> = arrayOfNulls(size)
}
