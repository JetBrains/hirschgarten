package org.jetbrains.bazel.sdkcompat

import com.dynatrace.hash4j.hashing.HashValue128
import com.dynatrace.hash4j.hashing.Hasher64
import com.dynatrace.hash4j.hashing.Hashing

@PublishedApi
internal val seededHashing: Hasher64 = Hashing.xxh3_64(42)

interface HashAdapter {
  fun putByteArray(value: ByteArray)

  fun putInt(value: Int)

  fun putByte(value: Byte)

  fun getAndReset(): HashValue128
}

// no xxh3_128 in 243
fun hashBytesTo128Bits(input: ByteArray): HashValue128 =
  HashValue128(Hashing.xxh3_64().hashBytesToLong(input), seededHashing.hashBytesToLong(input))

fun createHashStream128(): HashAdapter {
  val stream = Hashing.xxh3_64().hashStream()
  val stream2 = seededHashing.hashStream()
  return object : HashAdapter {
    override fun putByteArray(value: ByteArray) {
      stream.putByteArray(value)
      stream2.putByteArray(value)
    }

    override fun putInt(value: Int) {
      stream.putInt(value)
      stream2.putInt(value)
    }

    override fun putByte(value: Byte) {
      stream.putByte(value)
      stream2.putByte(value)
    }

    override fun getAndReset(): HashValue128 {
      val result = HashValue128(stream.asLong, stream2.asLong)
      stream.reset()
      stream2.reset()
      return result
    }
  }
}
