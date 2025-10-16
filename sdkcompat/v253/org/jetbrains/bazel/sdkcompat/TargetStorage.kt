package org.jetbrains.bazel.sdkcompat

import com.dynatrace.hash4j.hashing.HashValue128
import com.dynatrace.hash4j.hashing.Hashing

interface HashAdapter {
  fun putByteArray(value: ByteArray)

  fun putInt(value: Int)

  fun putByte(value: Byte)

  fun getAndReset(): HashValue128
}

// no xxh3_128 in 243
fun hashBytesTo128Bits(input: ByteArray): HashValue128 = Hashing.xxh3_128().hashBytesTo128Bits(input)

fun createHashStream128(): HashAdapter {
  val stream = Hashing.xxh3_128().hashStream()
  return object : HashAdapter {
    override fun putByteArray(value: ByteArray) {
      stream.putByteArray(value)
    }

    override fun putInt(value: Int) {
      stream.putInt(value)
    }

    override fun putByte(value: Byte) {
      stream.putByte(value)
    }

    override fun getAndReset(): HashValue128 {
      val result = stream.get()
      stream.reset()
      return result
    }
  }
}
