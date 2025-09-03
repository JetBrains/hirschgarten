package org.jetbrains.bazel.sync.workspace.languages.protobuf

import kotlinx.io.IOException
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bsp.protocol.ProtobufBuildTarget
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
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
      .mapNotNull { resolveSource(context, target, it) }
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
  ): SourceMapping? {
    val proto = target.protobufTargetInfo
    val absoluteSourcePath = context.pathsResolver.resolve(source).absolutePathString()
    return if (proto.protoSourceRoot == ".") {
      SourceMapping(source.relativePath, absoluteSourcePath)
    } else {
      val absoluteProtoSourceRoot = resolveProtoSourceRoot(context, proto.protoSourceRoot) ?: return null
      val importPath = absoluteSourcePath.removePrefix(absoluteProtoSourceRoot.absolutePathString())
        .removePrefix("/")
      SourceMapping(importPath, absoluteSourcePath)
    }
  }

  private fun resolveProtoSourceRoot(context: LanguagePluginContext, protoSourceRoot: String): Path? {
    // we don't want to resolve entire path to real because destination path might not exists
    val bazelOutPath = context.pathsResolver.workspaceRoot().resolve("bazel-out")
    return if (protoSourceRoot.startsWith("bazel-out")) {
      // bazel-out should be symlink - but handle every possible case
      val bazelOut = if (Files.isSymbolicLink(bazelOutPath)) {
        Files.readSymbolicLink(bazelOutPath)
      } else {
        bazelOutPath
      }
      val protoSourceRootWithoutBazelOut = protoSourceRoot.removePrefix("bazel-out/")
      return bazelOut.resolve(protoSourceRootWithoutBazelOut)
    } else {
      // toRealPath may and will fail when bazel haven't built proto sources symlinks
      try {
        context.pathsResolver.workspaceRoot().resolve(protoSourceRoot)
          .toRealPath()
      } catch (_: IOException) {
        null
      }
    }
  }
}
