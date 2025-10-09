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

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): ProtobufBuildTarget? {
    if (!target.hasProtobufTargetInfo()) {
      return null
    }
    val sources =
      target.protobufTargetInfo.sourceMappingsList
        .associate<BspTargetInfo.ProtobufSourceMapping, String, String> {
          it.importPath to context.pathsResolver.resolve(it.protoFile).absolutePathString()
        }
    return ProtobufBuildTarget(
      sources = sources,
      jvmBuildTarget = javaPlugin.createBuildTargetData(context, target),
    )
  }
}
