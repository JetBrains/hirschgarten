package org.jetbrains.bazel.sync.workspace.languages.protobuf

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.protocol.ProtobufBuildTarget

class ProtobufLanguagePlugin(private val javaPlugin: JavaLanguagePlugin) : LanguagePlugin<ProtobufBuildTarget> {

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.PROTOBUF)

  override suspend fun createBuildTargetData(
    context: LanguagePluginContext,
    target: BspTargetInfo.TargetInfo,
  ): ProtobufBuildTarget? {
    if (!target.hasProtobufTargetInfo()) {
      return null
    }
    val protoTarget = target.protobufTargetInfo
    return ProtobufBuildTarget(
      sources = resolveRelativeSources(context, target, protoTarget),
      jvmBuildTarget = javaPlugin.createBuildTargetData(context, target),
    )
  }

  private fun resolveRelativeSources(
    context: LanguagePluginContext,
    target: BspTargetInfo.TargetInfo,
    protoTarget: BspTargetInfo.ProtobufTargetInfo,
  ): Map<String, String> {
    return target.sourcesList.filter { it.relativePath.endsWith(".proto") }
      .associate { it.relativizeSourcePath(context, target, protoTarget) to it.resolveRelativePath(context) }
  }

  private fun BspTargetInfo.FileLocation.resolveRelativePath(context: LanguagePluginContext): String {
    return context.pathsResolver.workspaceRoot().resolve(this.relativePath).toString()
  }

  private fun BspTargetInfo.FileLocation.relativizeSourcePath(
    context: LanguagePluginContext,
    target: BspTargetInfo.TargetInfo,
    protoTarget: BspTargetInfo.ProtobufTargetInfo,
  ): String {
    val label = target.label().assumeResolved()
    val packagePrefix = label.packagePath.pathSegments.joinToString("/")
    val stripped = if (protoTarget.stripImportPrefix.isNullOrBlank()) {
      this.relativePath.removePrefix(packagePrefix)
        .removePrefix("/")
    } else {
      this.relativePath.removePrefix(packagePrefix)
        .removePrefix("/")
        .removePrefix(protoTarget.stripImportPrefix)
        .removePrefix("/")
    }
    return if (protoTarget.importPrefix.isNullOrBlank()) {
      "${packagePrefix}/${stripped}"
    } else {
      "${protoTarget.importPrefix}/${stripped}"
    }
  }

}
