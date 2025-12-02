package org.jetbrains.bazel.target

import org.h2.mvstore.DataUtils.readVarInt
import org.h2.mvstore.WriteBuffer
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.GoBuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.VoidBuildTarget
import java.nio.ByteBuffer
import java.nio.file.Path

/**
 * Binary serializers for BuildTargetData types to replace inefficient JSON serialization.
 * This reduces serialization overhead by 5-10x compared to JSON.
 */
internal object BinaryBuildTargetDataSerializer {

  fun serialize(data: BuildTargetData, buffer: WriteBuffer, rootDir: Path, filePathSuffix: String) {
    when (data) {
      is KotlinBuildTarget -> serializeKotlinBuildTarget(data, buffer, rootDir, filePathSuffix)
      is JvmBuildTarget -> serializeJvmBuildTarget(data, buffer, rootDir, filePathSuffix)
      is PythonBuildTarget -> serializePythonBuildTarget(data, buffer, rootDir, filePathSuffix)
      is ScalaBuildTarget -> serializeScalaBuildTarget(data, buffer, rootDir, filePathSuffix)
      is GoBuildTarget -> serializeGoBuildTarget(data, buffer, rootDir, filePathSuffix)
      is ProtobufBuildTarget -> serializeProtobufBuildTarget(data, buffer, rootDir, filePathSuffix)
      is VoidBuildTarget -> { /* no data to write */ }
    }
  }

  fun deserialize(classId: Int, buffer: ByteBuffer, rootDir: Path): BuildTargetData {
    return when (classId) {
      1 -> deserializeKotlinBuildTarget(buffer, rootDir)
      2 -> deserializePythonBuildTarget(buffer, rootDir)
      3 -> deserializeScalaBuildTarget(buffer, rootDir)
      4 -> deserializeJvmBuildTarget(buffer, rootDir)
      5 -> deserializeGoBuildTarget(buffer, rootDir)
      8 -> VoidBuildTarget
      9 -> deserializeProtobufBuildTarget(buffer, rootDir)
      else -> throw IllegalStateException("Unknown BuildTargetData class id: $classId")
    }
  }

  private fun serializeKotlinBuildTarget(data: KotlinBuildTarget, buffer: WriteBuffer, rootDir: Path, filePathSuffix: String) {
    buffer.writeString(data.languageVersion)
    buffer.writeString(data.apiVersion)
    buffer.writeStringList(data.kotlincOptions)
    buffer.writeLabelList(data.associates)
    buffer.put(if (data.jvmBuildTarget != null) 1 else 0)
    data.jvmBuildTarget?.let { serializeJvmBuildTarget(it, buffer, rootDir, filePathSuffix) }
  }

  private fun deserializeKotlinBuildTarget(buffer: ByteBuffer, rootDir: Path): KotlinBuildTarget {
    val languageVersion = buffer.readString()
    val apiVersion = buffer.readString()
    val kotlincOptions = buffer.readStringList()
    val associates = buffer.readLabelList()
    val hasJvmTarget = buffer.get() == 1.toByte()
    val jvmBuildTarget = if (hasJvmTarget) deserializeJvmBuildTarget(buffer, rootDir) else null
    return KotlinBuildTarget(languageVersion, apiVersion, kotlincOptions, associates, jvmBuildTarget)
  }

  private fun serializeJvmBuildTarget(data: JvmBuildTarget, buffer: WriteBuffer, rootDir: Path, filePathSuffix: String) {
    buffer.writeString(data.javaVersion)
    buffer.writeStringList(data.javacOpts)
    buffer.writePathList(data.binaryOutputs, filePathSuffix)
    buffer.writeStringMap(data.environmentVariables)
    buffer.writeNullableString(data.mainClass)
    buffer.writeStringList(data.jvmArgs)
    buffer.writeStringList(data.programArgs)
  }

  private fun deserializeJvmBuildTarget(buffer: ByteBuffer, rootDir: Path): JvmBuildTarget {
    val javaVersion = buffer.readString()
    val javacOpts = buffer.readStringList()
    val binaryOutputs = buffer.readPathList(rootDir)
    val environmentVariables = buffer.readStringMap()
    val mainClass = buffer.readNullableString()
    val jvmArgs = buffer.readStringList()
    val programArgs = buffer.readStringList()
    return JvmBuildTarget(null, javaVersion, javacOpts, binaryOutputs, environmentVariables, mainClass, jvmArgs, programArgs)
  }

  private fun serializePythonBuildTarget(data: PythonBuildTarget, buffer: WriteBuffer, rootDir: Path, filePathSuffix: String) {
    buffer.writeNullableString(data.version)
    buffer.writeNullablePath(data.interpreter, filePathSuffix)
    buffer.writeStringList(data.imports)
    buffer.put(if (data.isCodeGenerator) 1 else 0)
    buffer.writePathList(data.generatedSources, filePathSuffix)
    buffer.writePathList(data.sourceDependencies, filePathSuffix)
  }

  private fun deserializePythonBuildTarget(buffer: ByteBuffer, rootDir: Path): PythonBuildTarget {
    val version = buffer.readNullableString()
    val interpreter = buffer.readNullablePath(rootDir)
    val imports = buffer.readStringList()
    val isCodeGenerator = buffer.get() == 1.toByte()
    val generatedSources = buffer.readPathList(rootDir)
    val sourceDependencies = buffer.readPathList(rootDir)
    return PythonBuildTarget(version, interpreter, imports, isCodeGenerator, generatedSources, sourceDependencies)
  }

  private fun serializeScalaBuildTarget(data: ScalaBuildTarget, buffer: WriteBuffer, rootDir: Path, filePathSuffix: String) {
    buffer.writeString(data.scalaVersion)
    buffer.writePathList(data.sdkJars, filePathSuffix)
    buffer.put(if (data.jvmBuildTarget != null) 1 else 0)
    data.jvmBuildTarget?.let { serializeJvmBuildTarget(it, buffer, rootDir, filePathSuffix) }
    buffer.writeStringList(data.scalacOptions)
  }

  private fun deserializeScalaBuildTarget(buffer: ByteBuffer, rootDir: Path): ScalaBuildTarget {
    val scalaVersion = buffer.readString()
    val sdkJars = buffer.readPathList(rootDir)
    val hasJvmTarget = buffer.get() == 1.toByte()
    val jvmBuildTarget = if (hasJvmTarget) deserializeJvmBuildTarget(buffer, rootDir) else null
    val scalacOptions = buffer.readStringList()
    return ScalaBuildTarget(scalaVersion, sdkJars, jvmBuildTarget, scalacOptions)
  }

  private fun serializeGoBuildTarget(data: GoBuildTarget, buffer: WriteBuffer, rootDir: Path, filePathSuffix: String) {
    buffer.writeString(data.importPath)
    buffer.writePathList(data.generatedLibraries, filePathSuffix)
    buffer.writePathList(data.generatedSources, filePathSuffix)
    buffer.writeLabelList(data.libraryLabels)
  }

  private fun deserializeGoBuildTarget(buffer: ByteBuffer, rootDir: Path): GoBuildTarget {
    val importPath = buffer.readString()
    val generatedLibraries = buffer.readPathList(rootDir)
    val generatedSources = buffer.readPathList(rootDir)
    val libraryLabels = buffer.readLabelList()
    return GoBuildTarget(null, importPath, generatedLibraries, generatedSources, libraryLabels)
  }

  private fun serializeProtobufBuildTarget(data: ProtobufBuildTarget, buffer: WriteBuffer, rootDir: Path, filePathSuffix: String) {
    buffer.putVarInt(data.sources.size)
    for ((key, value) in data.sources) {
      buffer.writeString(key)
      buffer.writeString(value)
    }
    buffer.put(if (data.jvmBuildTarget != null) 1 else 0)
    data.jvmBuildTarget?.let { serializeJvmBuildTarget(it, buffer, rootDir, filePathSuffix) }
  }

  private fun deserializeProtobufBuildTarget(buffer: ByteBuffer, rootDir: Path): ProtobufBuildTarget {
    val size = readVarInt(buffer)
    val sources = LinkedHashMap<String, String>(size)
    repeat(size) {
      sources[buffer.readString()] = buffer.readString()
    }
    val hasJvmTarget = buffer.get() == 1.toByte()
    val jvmBuildTarget = if (hasJvmTarget) deserializeJvmBuildTarget(buffer, rootDir) else null
    return ProtobufBuildTarget(sources, jvmBuildTarget)
  }

  // Helper extension functions
  private fun WriteBuffer.writeStringList(list: List<String>) {
    putVarInt(list.size)
    for (item in list) {
      writeString(item)
    }
  }

  private fun ByteBuffer.readStringList(): List<String> {
    val size = readVarInt(this)
    return if (size == 0) emptyList() else Array(size) { readString() }.asList()
  }

  private fun WriteBuffer.writeLabelList(list: List<Label>) {
    putVarInt(list.size)
    for (item in list) {
      writeString(item.toString())
    }
  }

  private fun ByteBuffer.readLabelList(): List<Label> {
    val size = readVarInt(this)
    return if (size == 0) emptyList() else Array(size) { Label.parse(readString()) }.asList()
  }

  private fun WriteBuffer.writePathList(list: List<Path>, filePathSuffix: String) {
    putVarInt(list.size)
    for (path in list) {
      writePath(path.toString(), filePathSuffix, this)
    }
  }

  private fun ByteBuffer.readPathList(rootDir: Path): List<Path> {
    val size = readVarInt(this)
    return if (size == 0) emptyList() else Array(size) { readPath(this, rootDir) }.asList()
  }

  private fun WriteBuffer.writeStringMap(map: Map<String, String>) {
    putVarInt(map.size)
    for ((key, value) in map) {
      writeString(key)
      writeString(value)
    }
  }

  private fun ByteBuffer.readStringMap(): Map<String, String> {
    val size = readVarInt(this)
    if (size == 0) return emptyMap()
    val result = LinkedHashMap<String, String>(size)
    repeat(size) {
      result[readString()] = readString()
    }
    return result
  }

  private fun WriteBuffer.writeNullableString(value: String?) {
    if (value == null) {
      put(0)
    } else {
      put(1)
      writeString(value)
    }
  }

  private fun ByteBuffer.readNullableString(): String? {
    return if (get() == 0.toByte()) null else readString()
  }

  private fun WriteBuffer.writeNullablePath(path: Path?, filePathSuffix: String) {
    if (path == null) {
      put(0)
    } else {
      put(1)
      writePath(path.toString(), filePathSuffix, this)
    }
  }

  private fun ByteBuffer.readNullablePath(rootDir: Path): Path? {
    return if (get() == 0.toByte()) null else readPath(this, rootDir)
  }
}

