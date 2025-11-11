package org.jetbrains.bazel.sync_new.codec

import com.dynatrace.hash4j.hashing.HashValue128

fun CodecBuilder.ofPrimitiveLong(): Codec<Long> = codecOf(
  encode = { _, buffer, value -> buffer.writeInt64(value) },
  decode = { _, buffer -> buffer.readInt64() },
)

fun CodecBuilder.ofHashValue128(): Codec<HashValue128> = codecOf(
  encode = { _, buffer, value ->
    buffer.writeInt64(value.mostSignificantBits)
    buffer.writeInt64(value.leastSignificantBits)
  },
  decode = { _, buffer ->
    val hi = buffer.readInt64()
    val lo = buffer.readInt64()
    HashValue128(hi, lo)
  },
)
