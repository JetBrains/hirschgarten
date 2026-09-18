@file:Suppress("IO_FILE_USAGE")

package org.jetbrains.bazel.clion.debug

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.clion.workspace.CcResolveConfiguration
import org.jetbrains.bazel.clion.workspace.encode
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.toExecrootPath
import java.io.File

internal val bazelDebugGson: Gson = bazelGson.newBuilder()
  .setPrettyPrinting()
  .registerSerializer(::serializeOutputLocation)
  .registerSerializer(::serializeOutputLocations)
  .registerSerializer(::serializeWorkspaceConfigurationId)
  .registerSerializer(::serializeTargetKey)
  .registerSerializer(::serializeTargetKind)
  .registerSerializer(::serializeDependencyLabel)
  .registerSerializer(::serializeCcIdentifier)
  .registerSerializer(::serializeFile)
  .registerSerializer(::serializeVirtualFile)
  .create()

private inline fun <reified T : Any> GsonBuilder.registerSerializer(crossinline body: (JsonWriter, T) -> Unit): GsonBuilder {
  registerTypeHierarchyAdapter(T::class.java, object : TypeAdapter<T?>() {

    override fun read(reader: JsonReader): T = throw NotImplementedError()

    override fun write(writer: JsonWriter, value: T?) {
      if (value == null) writer.nullValue() else body(writer, value)
    }
  })

  return this
}

private fun serializeOutputLocation(writer: JsonWriter, value: OutputLocation) {
  writer.value(value.toExecrootPath())
}

private fun serializeOutputLocations(writer: JsonWriter, value: OutputLocationCollection) {
  writer.beginArray()
  value.getOutputLocations().forEach { writer.value(it.toExecrootPath()) }
  writer.endArray()
}

private fun serializeWorkspaceConfigurationId(writer: JsonWriter, value: WorkspaceConfigurationId) {
  if (value.shortChecksum == null) {
    writer.nullValue()
  } else {
    writer.value(value.shortChecksum)
  }
}

private fun serializeTargetKey(writer: JsonWriter, value: WorkspaceTargetKey) {
  writer.beginObject()
  writer.name("label")
  writer.value(value.label.toString())
  writer.name("configuration")
  serializeWorkspaceConfigurationId(writer, value.configuration)
  writer.name("aspect_ids")
  writer.beginArray()
  value.aspectIds.ids.forEach(writer::value)
  writer.endArray()
  writer.endObject()
}

private fun serializeTargetKind(writer: JsonWriter, value: TargetKind) {
  writer.beginObject()
  writer.name("rule")
  writer.value(value.kind)
  writer.name("rule_type")
  writer.value(value.ruleType.name)
  writer.name("language_classes")
  writer.beginArray()
  value.languageClasses.forEach { writer.value(it.toString()) }
  writer.endArray()
  writer.endObject()
}

private fun serializeDependencyLabel(writer: JsonWriter, value: DependencyLabel) {
  writer.beginObject()
  writer.name("kind")
  writer.value(value.kind.name)
  writer.name("target")
  serializeTargetKey(writer, value.targetKey)
  writer.endObject()
}

private fun serializeCcIdentifier(writer: JsonWriter, value: CcResolveConfiguration.Identifier) {
  writer.value(value.encode())
}

private fun serializeFile(writer: JsonWriter, value: File) {
  writer.value(value.path)
}

private fun serializeVirtualFile(writer: JsonWriter, value: VirtualFile) {
  writer.value(value.path)
}
