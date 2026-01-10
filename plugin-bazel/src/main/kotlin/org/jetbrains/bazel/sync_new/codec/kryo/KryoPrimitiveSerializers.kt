package org.jetbrains.bazel.sync_new.codec.kryo

import com.dynatrace.hash4j.hashing.HashValue128
import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.Serializer
import com.esotericsoftware.kryo.kryo5.io.Input
import com.esotericsoftware.kryo.kryo5.io.Output
import org.jetbrains.bazel.label.AllPackagesBeneath
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.AllRuleTargetsAndFiles
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.PackageType
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.label.RepoType
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.label.SyntheticLabel
import org.jetbrains.bazel.label.TargetType
import org.jetbrains.bazel.sync_new.codec.BazelPathCodec
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.codec.LabelCodec
import org.jetbrains.bazel.sync_new.codec.NIOPathCodec
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import java.nio.file.Path

fun Kryo.registerPrimitiveSerializers() {
  register(HashValue128::class.java, HashValue128Serializer, 1193690579)

  addDefaultSerializer(Label::class.java, CodecKryoSerializer.of(LabelCodec))
  register(ResolvedLabel::class.java, 2134527274)
  register(RelativeLabel::class.java, 1243654471)
  register(SyntheticLabel::class.java, 1243654471)

  addDefaultSerializer(BazelPath::class.java, CodecKryoSerializer.of(BazelPathCodec))
  register(BazelPath.Absolute::class.java, 950368927)
  register(BazelPath.MainWorkspace::class.java, 1889420056)
  register(BazelPath.ExternalWorkspace::class.java, 975726949)

  addDefaultSerializer(Path::class.java, CodecKryoSerializer.of(NIOPathCodec))
}

object HashValue128Serializer : Serializer<HashValue128>() {
  override fun write(
    kryo: Kryo,
    output: Output,
    obj: HashValue128,
  ) {
    output.writeLong(obj.mostSignificantBits)
    output.writeLong(obj.leastSignificantBits)
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out HashValue128>,
  ): HashValue128 {
    val hi = input.readLong()
    val lo = input.readLong()
    return HashValue128(hi, lo)
  }
}

object KryoCodecContext : CodecContext

abstract class CodecKryoSerializer<T> : Serializer<T>() {
  abstract val codec: Codec<T>

  override fun write(kryo: Kryo, output: Output, obj: T) {
    codec.encode(KryoCodecContext, KryoWriteCodecBuffer(output), obj)
  }

  override fun read(
    kryo: Kryo,
    input: Input,
    type: Class<out T>,
  ): T? {
    return codec.decode(KryoCodecContext, KryoReadCodecBuffer(input))
  }

  companion object {
    fun <T> of(codec: Codec<T>): CodecKryoSerializer<T> = object : CodecKryoSerializer<T>() {
      override val codec: Codec<T> = codec
    }
  }
}

class KryoNIOPathSerializer : CodecKryoSerializer<Path>() {
  override val codec: Codec<Path>
    get() = NIOPathCodec

}
