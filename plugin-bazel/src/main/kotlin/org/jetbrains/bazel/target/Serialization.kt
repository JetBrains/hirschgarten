package org.jetbrains.bazel.target

import com.dynatrace.hash4j.hashing.HashValue128
import it.unimi.dsi.fastutil.objects.ObjectArraySet
import org.h2.mvstore.DataUtils.readVarInt
import org.h2.mvstore.MVMap
import org.h2.mvstore.WriteBuffer
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.AllPackagesBeneath
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.AllRuleTargetsAndFiles
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bsp.protocol.PartialBuildTarget
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

internal fun WriteBuffer.writeString(value: String) {
  if (value.isEmpty()) {
    putVarInt(0)
    return
  }

  val valueBytes = value.encodeToByteArray()
  putVarInt(valueBytes.size)
  put(valueBytes)
}

internal fun ByteBuffer.readString(): String {
  val size = readVarInt(this)
  if (size == 0) {
    return ""
  }

  val bytes = ByteArray(size)
  get(bytes)
  return String(bytes)
}

internal fun writeResolvedLabel(buffer: WriteBuffer, label: ResolvedLabel) {
  when (val repo = label.repo) {
    Main -> buffer.put(0)
    is Canonical -> {
      buffer.put(1)
      buffer.writeString(repo.repoName)
    }

    is Apparent -> {
      buffer.put(2)
      buffer.writeString(repo.repoName)
    }
  }

  val packagePath = label.packagePath
  when (packagePath) {
    is AllPackagesBeneath -> buffer.put(0)
    is Package -> buffer.put(1)
  }
  buffer.putVarInt(packagePath.pathSegments.size)
  for (value in packagePath.pathSegments) {
    buffer.writeString(value)
  }

  when (val target = label.target) {
    AmbiguousEmptyTarget -> buffer.put(0)
    AllRuleTargets -> buffer.put(1)
    AllRuleTargetsAndFiles -> buffer.put(2)
    is SingleTarget -> {
      buffer.put(3)
      buffer.writeString(target.targetName)
    }
  }
}

internal fun readResolvedLabel(buffer: ByteBuffer): ResolvedLabel {
  val repoType = buffer.get()
  val repo =
    when (repoType.toInt()) {
      0 -> Main
      1 -> Canonical.createCanonicalOrMain(buffer.readString())
      2 -> Apparent(buffer.readString())
      else -> throw IllegalStateException("Unexpected repo type $repoType")
    }

  val packageType = buffer.get()
  val packagePath =
    when (packageType.toInt()) {
      0 -> AllPackagesBeneath(Array(readVarInt(buffer)) { buffer.readString() }.asList())
      1 -> Package(Array(readVarInt(buffer)) { buffer.readString() }.asList())
      else -> throw IllegalStateException("Unexpected package type $packageType")
    }

  val targetType = buffer.get()
  val target =
    when (targetType.toInt()) {
      0 -> AmbiguousEmptyTarget
      1 -> AllRuleTargets
      2 -> AllRuleTargetsAndFiles
      3 -> SingleTarget(buffer.readString())
      else -> throw IllegalStateException("Unexpected target type $targetType")
    }

  return ResolvedLabel(repo = repo, packagePath = packagePath, target = target)
}

internal fun createIdToBuildMapType(filePathSuffix: String, rootDir: Path): MVMap.Builder<HashValue128, PartialBuildTarget> {
  val mapBuilder = MVMap.Builder<HashValue128, PartialBuildTarget>()
  mapBuilder.setKeyType(HashValue128KeyDataType)
  mapBuilder.setValueType(
    createAnyValueDataType<PartialBuildTarget>(
      writer = { buffer, item ->
        writeResolvedLabel(buffer, item.id as ResolvedLabel)

        buffer.putVarInt(item.tags.size)
        for (tag in item.tags) {
          buffer.writeString(tag)
        }

        writeTargetKind(item.kind, buffer)
        writePath(path = item.baseDirectory.invariantSeparatorsPathString, filePathSuffix = filePathSuffix, buffer = buffer)
        buffer.put(if (item.noBuild) 1 else 0)

        val targetData = item.data
        if (targetData == null) {
          buffer.putVarInt(0)
        } else {
          val aClass = targetData.javaClass
          BuildDataTargetTypeRegistry.writeClassId(aClass, buffer)
          val data = bazelGson.toJson(targetData, aClass).encodeToByteArray()
          buffer.putVarInt(data.size)
          buffer.put(data)
        }
      },
      reader = { buffer ->
        val id = readResolvedLabel(buffer)
        val tags = Array(readVarInt(buffer)) { buffer.readString() }.asList()
        val kind = readTargetKind(buffer)

        val baseDirectory = readPath(buffer, rootDir)
        val noBuild = buffer.get() == 1.toByte()

        val typeId = buffer.get().toInt()
        val data =
          if (typeId == 0) {
            null
          } else {
            val aClass = BuildDataTargetTypeRegistry.getClass(typeId)
            val dataSize = readVarInt(buffer)
            val encodedData = ByteArray(dataSize)
            buffer.get(encodedData)
            bazelGson.fromJson(encodedData.decodeToString(), aClass)
          }
        PartialBuildTarget(id = id, tags = tags, kind = kind, baseDirectory = baseDirectory, data = data, noBuild = noBuild)
      },
    ),
  )
  return mapBuilder
}

private const val RELATIVE_PATH = 1.toByte()
private const val ROOT_PATH = 2.toByte()
private const val ABSOLUTE_PATH = 0.toByte()

private fun writePath(
  path: String,
  filePathSuffix: String,
  buffer: WriteBuffer,
) {
  if (path.startsWith(filePathSuffix)) {
    buffer.put(RELATIVE_PATH)
    buffer.writeString(path.substring(filePathSuffix.length))
  } else if (path.length == (filePathSuffix.length - 1) && filePathSuffix.startsWith(path)) {
    buffer.put(ROOT_PATH)
  } else {
    buffer.put(ABSOLUTE_PATH)
    buffer.writeString(path)
  }
}

private fun readPath(buffer: ByteBuffer, rootDir: Path): Path =
  when (val pathKind = buffer.get()) {
    RELATIVE_PATH -> rootDir.resolve(buffer.readString())
    ROOT_PATH -> rootDir
    ABSOLUTE_PATH -> Path.of(buffer.readString())
    else -> throw IllegalStateException("Unexpected path kind $pathKind")
  }

private fun writeTargetKind(kind: TargetKind, buffer: WriteBuffer) {
  buffer.writeString(kind.kindString)
  buffer.putVarInt(kind.languageClasses.size)
  for (languageClass in kind.languageClasses) {
    buffer.put(languageClass.ordinal.toByte())
  }
  buffer.put(kind.ruleType.ordinal.toByte())
}

private fun readTargetKind(buffer: ByteBuffer): TargetKind {
  val kindString = buffer.readString()
  val languageClasses = ObjectArraySet<LanguageClass>(Array(readVarInt(buffer)) { LanguageClass.entries[buffer.get().toInt()] })
  val ruleType = RuleType.entries[buffer.get().toInt()]
  return TargetKind(kindString = kindString, languageClasses = languageClasses, ruleType = ruleType)
}
