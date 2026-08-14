package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.h2.mvstore.DataUtils
import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.BasicDataType
import org.h2.mvstore.type.DataType
import java.nio.ByteBuffer

internal const val DEFAULT_BUFFER_SIZE = 1024 * 1024

/**
 * Frame of each value put inside MVStore map, it offers graceful fallback in
 * case of failure or schema version divergence.
 */
internal sealed interface ValueFrame {
  class Present(val version: Int, val payload: ByteArray) : ValueFrame
  data object Outdated : ValueFrame
  data object Error : ValueFrame
}

private const val DISCRIMINATOR_ERROR: Byte = 0
private const val DISCRIMINATOR_PRESENT: Byte = 1
private const val DISCRIMINATOR_OUTDATED: Byte = 2

internal fun createFrameDataType(): DataType<ValueFrame> =
  object : BasicDataType<ValueFrame>() {
    override fun getMemory(obj: ValueFrame): Int = when (obj) {
      is ValueFrame.Present -> obj.payload.size + 16
      else -> 1
    }

    override fun write(buff: WriteBuffer, obj: ValueFrame) {
      when (obj) {
        is ValueFrame.Present -> {
          buff.put(DISCRIMINATOR_PRESENT)
          buff.putVarInt(obj.version)
          buff.putVarInt(obj.payload.size)
          buff.put(obj.payload)
        }
        ValueFrame.Outdated -> buff.put(DISCRIMINATOR_OUTDATED)
        ValueFrame.Error -> buff.put(DISCRIMINATOR_ERROR)
      }
    }

    override fun read(buff: ByteBuffer): ValueFrame {
      return when (val discriminator = buff.get()) {
        DISCRIMINATOR_PRESENT -> {
          val version = DataUtils.readVarInt(buff)
          val length = DataUtils.readVarInt(buff)
          val payload = ByteArray(length)
          buff.get(payload)
          ValueFrame.Present(version, payload)
        }
        DISCRIMINATOR_ERROR -> ValueFrame.Error
        DISCRIMINATOR_OUTDATED -> ValueFrame.Outdated
        else -> error("Invalid value discriminator: $discriminator")
      }
    }

    override fun createStorage(size: Int): Array<ValueFrame?> = arrayOfNulls(size)
  }
