package org.jetbrains.bazel.target

import com.google.gson.Gson
import com.intellij.openapi.diagnostic.logger
import org.h2.mvstore.DataUtils.readVarInt
import org.h2.mvstore.MVMap
import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.LongDataType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.PartialBuildTarget
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.EnumSet
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.path.invariantSeparatorsPathString

private val LOG = logger<PartialBuildTarget>()

@get:ApiStatus.Internal
val targetUtilsGson: Gson = bazelGson.newBuilder()
  .registerTypeAdapter(SourceFileCollection::class.java, SourceFileCollectionGsonAdapter)
  .create()

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

internal fun writeLabel(buffer: WriteBuffer, label: Label) {
  buffer.writeString(label.toString())
}

internal fun readLabel(buffer: ByteBuffer): Label {
  return Label.parse(buffer.readString())
}

internal fun createIdToBuildMapType(filePathSuffix: String, rootDir: Path): MVMap.Builder<Long, PartialBuildTarget> {
  val mapBuilder = MVMap.Builder<Long, PartialBuildTarget>()
  mapBuilder.setKeyType(LongDataType.INSTANCE)
  mapBuilder.setValueType(
    createAnyValueDataType<PartialBuildTarget>(
      writer = { buffer, item ->
        writeLabel(buffer, item.id)
        writeTargetKind(item.kind, buffer)
        writePath(path = item.baseDirectory.invariantSeparatorsPathString, filePathSuffix = filePathSuffix, buffer = buffer)
        buffer.put(if (item.isManual) 1 else 0)
        buffer.put(if (item.isWorkspace) 1 else 0)

        buffer.putVarInt(item.data.size)
        for (targetData in item.data) {
          val aClass = targetData.javaClass
          BuildDataTargetTypeRegistry.writeClassId(aClass, buffer)
          val data = gzip(targetUtilsGson.toJson(targetData, aClass).encodeToByteArray())
          buffer.putVarInt(data.size)
          buffer.put(data)
        }
      },
      reader = { buffer ->
        val id = readLabel(buffer)
        val kind = readTargetKind(buffer)
        val baseDirectory = readPath(buffer, rootDir)
        val isManual = buffer.get() == 1.toByte()
        val isWorkspace = buffer.get() == 1.toByte()

        val dataCount = readVarInt(buffer)
        val data = (0 until dataCount).map {
          val typeId = buffer.get().toInt()
          val aClass = BuildDataTargetTypeRegistry.getClass(typeId)
          val dataSize = readVarInt(buffer)
          val encodedData = ByteArray(dataSize)
          buffer.get(encodedData)
          targetUtilsGson.fromJson(ungzip(encodedData).decodeToString(), aClass)
        }
        PartialBuildTarget(
          id = id,
          kind = kind,
          baseDirectory = baseDirectory,
          data = data,
          isManual = isManual,
          isWorkspace = isWorkspace
        )
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
  buffer.writeString(kind.kind)
  buffer.putVarInt(kind.languageClasses.size)
  for (languageClass in kind.languageClasses) {
    buffer.put(languageClass.serialId.toByte())
  }
  buffer.put(kind.ruleType.ordinal.toByte())
}

private fun readTargetKind(buffer: ByteBuffer): TargetKind {
  val kindString = buffer.readString()
  val languageClasses = EnumSet.noneOf(LanguageClass::class.java)
  val languageClassCount = readVarInt(buffer)
  repeat(languageClassCount) {
    val serialId = buffer.get().toInt()
    val languageClass = LanguageClass.fromSerialId(serialId)
    if (languageClass != null) {
      languageClasses.add(languageClass)
    } else {
      // BAZEL-2292: Log unknown serialIds to diagnose potential database corruption
      LOG.debug("Unknown LanguageClass serialId $serialId for kind '$kindString' - possible database corruption")
    }
  }
  val ruleType = RuleType.entries[buffer.get().toInt()]
  return TargetKind(kind = kindString, languageClasses = languageClasses, ruleType = ruleType)
}

/** Returns a decompressed byte array of the given content. */
private fun ungzip(data: ByteArray): ByteArray {
  val byteArrayInputStream = data.inputStream()
  return GZIPInputStream(byteArrayInputStream).use { it.readAllBytes() }
}

/** Returns a compressed byte array of the given content. */
private fun gzip(content: ByteArray): ByteArray {
  val byteArrayOutputStream = ByteArrayOutputStream()
  GZIPOutputStream(byteArrayOutputStream).use { it.write(content) }
  return byteArrayOutputStream.toByteArray()
}

