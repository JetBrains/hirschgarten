package org.jetbrains.bazel.sync_new.codec

import com.dynatrace.hash4j.hashing.HashValue128
import com.esotericsoftware.kryo.kryo5.io.Input
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
import org.jetbrains.bazel.sync_new.graph.impl.BazelPath
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun CodecBuilder.ofLong(): Codec<Long> = codecOf(
  encode = { _, buffer, value -> buffer.writeVarLong(value) },
  decode = { _, buffer -> buffer.readVarLong() }
)

fun CodecBuilder.ofInt(): Codec<Int> = codecOf(
  encode = { _, buffer, value -> buffer.writeVarInt(value) },
  decode = { _, buffer -> buffer.readVarInt() },
)

// using varlong in this case is just waste of resources
fun CodecBuilder.ofHash128(): Codec<HashValue128> = codecOf(
  encode = { _, buffer, value ->
    buffer.writeInt64(value.mostSignificantBits)
    buffer.writeInt64(value.leastSignificantBits)
  },
  decode = { _, buffer ->
    val hi = buffer.readInt64()
    val lo = buffer.readInt64()
    HashValue128(hi, lo)
  },
  size = { _, _ -> 2 * Long.SIZE_BYTES },
)

val hash128Codec: Codec<HashValue128> = codecBuilderOf().ofHash128()

fun CodecBuilder.ofString(): Codec<String> = codecOf(
  encode = { _, buffer, value -> buffer.writeString(value) },
  decode = { _, buffer -> buffer.readString() }
)

fun CodecBuilder.ofPath(): Codec<Path> = codecOf(
  encode = { _, buffer, value -> buffer.writeString(value.absolutePathString()) },
  decode = { _, buffer -> Path.of(buffer.readString()) }
)

// TODO: check if it is actually correct, or should it be serialized differently
//  that's why this codec is versioned
fun CodecBuilder.ofLabel(): Codec<Label> = LabelCodec

object LabelCodec : Codec<Label> {
  private const val CODEC_VERSION = 1

  override fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: Label,
  ) {
    buffer.writeVarInt(CODEC_VERSION)
    when (value) {
      is RelativeLabel -> {
        buffer.writeInt8(1)
        writePackagePath(buffer, value.packagePath)
        writeTargetType(buffer, value.target)
      }

      is ResolvedLabel -> {
        buffer.writeInt8(2)
        writeRepoType(buffer, value.repo)
        writePackagePath(buffer, value.packagePath)
        writeTargetType(buffer, value.target)
      }

      is SyntheticLabel -> {
        buffer.writeInt8(3)
        writeTargetType(buffer, value.target)
      }
    }
  }

  override fun decode(
    ctx: CodecContext,
    buffer: CodecBuffer,
  ): Label {
    val version = buffer.readVarInt()
    check(version == CODEC_VERSION) { "Unsupported codec version: $version, expected: $CODEC_VERSION" }

    val labelType = buffer.readInt8().toInt()
    return when (labelType) {
      1 -> {
        val packagePath = readPackagePath(buffer)
        val target = readTargetType(buffer)
        RelativeLabel(packagePath, target)
      }

      2 -> {
        val repo = readRepoType(buffer)
        val packagePath = readPackagePath(buffer)
        val target = readTargetType(buffer)
        ResolvedLabel(repo, packagePath, target)
      }

      3 -> {
        val target = readTargetType(buffer)
        SyntheticLabel(target)
      }

      else -> error("Unknown label type: $labelType")
    }
  }

  private fun writePackagePath(buffer: CodecBuffer, packageType: PackageType) {
    when (packageType) {
      is AllPackagesBeneath -> {
        buffer.writeInt8(1)
      }

      is Package -> {
        buffer.writeInt8(2)
      }
    }
    val pathSegments = packageType.pathSegments
    buffer.writeVarInt(pathSegments.size)
    for (segment in pathSegments) {
      buffer.writeString(segment)
    }
  }

  private fun writeTargetType(buffer: CodecBuffer, targetType: TargetType) {
    when (targetType) {
      AllRuleTargets -> {
        buffer.writeInt8(1)
      }

      AllRuleTargetsAndFiles -> {
        buffer.writeInt8(2)
      }

      AmbiguousEmptyTarget -> {
        buffer.writeInt8(3)
      }

      is SingleTarget -> {
        buffer.writeInt8(4)
        buffer.writeString(targetType.targetName)
      }
    }
  }

  private fun writeRepoType(buffer: CodecBuffer, repoType: RepoType) {
    when (repoType) {
      is Apparent -> {
        buffer.writeInt8(1)
        buffer.writeString(repoType.repoName)
      }

      is Canonical -> {
        buffer.writeInt8(2)
        buffer.writeString(repoType.repoName)
      }

      Main -> {
        buffer.writeInt8(3)
      }
    }
  }

  private fun readPackagePath(buffer: CodecBuffer): PackageType {
    val packageType = buffer.readInt8().toInt()
    val segmentsCount = buffer.readVarInt()
    val pathSegments = List(segmentsCount) { buffer.readString() }

    return when (packageType) {
      1 -> AllPackagesBeneath(pathSegments)
      2 -> Package(pathSegments)
      else -> error("Unknown package type: $packageType")
    }
  }

  private fun readTargetType(buffer: CodecBuffer): TargetType {
    val targetType = buffer.readInt8().toInt()
    return when (targetType) {
      1 -> AllRuleTargets
      2 -> AllRuleTargetsAndFiles
      3 -> AmbiguousEmptyTarget
      4 -> {
        val targetName = buffer.readString()
        SingleTarget(targetName)
      }

      else -> error("Unknown target type: $targetType")
    }
  }

  private fun readRepoType(buffer: CodecBuffer): RepoType {
    val repoType = buffer.readInt8().toInt()
    return when (repoType) {
      1 -> {
        val repoName = buffer.readString()
        Apparent(repoName)
      }

      2 -> {
        val repoName = buffer.readString()
        Canonical.createCanonicalOrMain(repoName)
      }

      3 -> Main
      else -> error("Unknown repo type: $repoType")
    }
  }

}

object BazelPathCodec : Codec<BazelPath> {
  private const val CODEC_VERSION = 1

  override fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: BazelPath,
  ) {
    buffer.writeVarInt(CODEC_VERSION)
    when (value) {
      is BazelPath.Absolute -> {
        buffer.writeInt8(1)
        buffer.writeString(value.path)
      }

      is BazelPath.ExternalWorkspace -> {
        buffer.writeInt8(2)
        buffer.writeString(value.relative)
        buffer.writeString(value.rootExecutionPathFragment)
        buffer.writeBoolean(value.isSource)
      }

      is BazelPath.MainWorkspace -> {
        buffer.writeInt8(3)
        buffer.writeString(value.relative)
      }
    }
  }

  override fun decode(
    ctx: CodecContext,
    buffer: CodecBuffer,
  ): BazelPath {
    val version = buffer.readVarInt()
    check(version == CODEC_VERSION)
    val pathType = buffer.readInt8().toInt()
    return when (pathType) {
      1 -> {
        val path = buffer.readString()
        BazelPath.Absolute(path)
      }

      2 -> {
        val relative = buffer.readString()
        val rootExecutionPathFragment = buffer.readString()
        val isSource = buffer.readBoolean()
        BazelPath.ExternalWorkspace(rootExecutionPathFragment, relative, isSource)
      }

      3 -> {
        val relative = buffer.readString()
        BazelPath.MainWorkspace(relative)
      }

      else -> error("Unknown path type: $pathType")
    }
  }

}

object NIOPathCodec : Codec<Path> {
  override fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: Path,
  ) {
    buffer.writeString(value.absolutePathString())
  }

  override fun decode(
    ctx: CodecContext,
    buffer: CodecBuffer,
  ): Path {
    return Path.of(buffer.readString())
  }

}
