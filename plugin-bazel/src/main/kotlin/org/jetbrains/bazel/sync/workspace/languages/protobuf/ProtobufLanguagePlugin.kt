package org.jetbrains.bazel.sync.workspace.languages.protobuf

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import kotlin.io.path.absolutePathString

class ProtobufLanguagePlugin(private val javaPlugin: JavaLanguagePlugin) : LanguagePlugin<ProtobufBuildTarget> {

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.PROTOBUF)

  override suspend fun createBuildTargetData(
    context: LanguagePluginContext,
    target: BspTargetInfo.TargetInfo,
  ): ProtobufBuildTarget? {
    if (!target.hasProtobufTargetInfo()) {
      return null
    }
    val sources = target.protobufTargetInfo.directProtoSourcesList
      .map { resolveSource(context, target, it) }
      .associateBy { it.importPath }
      .mapValues { it.value.absoluteSourcePath }
    return ProtobufBuildTarget(
      sources = sources,
      jvmBuildTarget = javaPlugin.createBuildTargetData(context, target),
    )
  }

  data class SourceMapping(val importPath: String, val absoluteSourcePath: String)

  private fun resolveSource(
    context: LanguagePluginContext,
    target: BspTargetInfo.TargetInfo,
    source: BspTargetInfo.FileLocation,
  ): SourceMapping {
    val proto = target.protobufTargetInfo
    val absoluteSourcePath = context.pathsResolver.resolve(source).absolutePathString()
    return if (proto.protoSourceRoot == ".") {
      SourceMapping(source.relativePath, absoluteSourcePath)
    } else {
      val absoluteProtoSourceRoot = context.pathsResolver.workspaceRoot().resolve(proto.protoSourceRoot)
        .toRealPath()
        .absolutePathString()
      val importPath = absoluteSourcePath.removePrefix(absoluteProtoSourceRoot)
        .removePrefix("/")
      SourceMapping(importPath, absoluteSourcePath)
    }
  }
}
