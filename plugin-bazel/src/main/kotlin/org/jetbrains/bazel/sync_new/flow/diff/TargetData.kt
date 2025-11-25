package org.jetbrains.bazel.sync_new.flow.diff

import com.dynatrace.hash4j.hashing.HashValue128
import org.jetbrains.bazel.sync_new.codec.codecOf
import org.jetbrains.bazel.sync_new.codec.readString
import org.jetbrains.bazel.sync_new.codec.writeString

internal data class TargetData(
  val hash: HashValue128,
  val path: String?
) {
  companion object {
    internal val codec = codecOf(
      encode = { _, buffer, value ->
        buffer.writeInt64(value.hash.mostSignificantBits)
        buffer.writeInt64(value.hash.leastSignificantBits)
        if (value.path == null) {
          buffer.writeInt8(0)
        } else {
          buffer.writeInt8(1)
          buffer.writeString(value.path)
        }
      },
      decode = { _, buffer ->
        val hashHi = buffer.readInt64()
        val hashLo = buffer.readInt64()
        val path = if (buffer.readInt8().toInt() == 0) {
          null
        } else {
          buffer.readString()
        }
        TargetData(
          hash = HashValue128(hashHi, hashLo),
          path = path
        )
      },
    )

    fun ofTargetHash(hash: TargetHash) = TargetData(
      hash = hash.hash,
      path = hash.path
    )
  }
}
