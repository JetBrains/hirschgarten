package org.jetbrains.bazel.sync_new.codec

import com.dynatrace.hash4j.hashing.HashValue128
import org.jetbrains.bazel.label.Label

fun CodecBuilder.ofPrimitiveLong(): Codec<Long> = codecOf(
  encode = { _, buffer, value -> buffer.writeVarLong(value) },
  decode = { _, buffer -> buffer.readVarLong() },
  size = { _, _ -> Long.SIZE_BYTES }
)

// using varlong in this case is just waste of resources
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
  size = { _, _ -> 2 * Long.SIZE_BYTES }
)

// TODO: check if it is actually correct, or should it be serialized differently
//  that's why this codec is versioned
fun CodecBuilder.ofLabel(): Codec<Label> = versionedCodecOf(
  version = 1,
  encode = { _, buffer, value ->
    buffer.writeString(value.toString())
  },
  decode = { _, buffer, version ->
    check(version == 1)
    Label.parse(buffer.readString())
  },
)
