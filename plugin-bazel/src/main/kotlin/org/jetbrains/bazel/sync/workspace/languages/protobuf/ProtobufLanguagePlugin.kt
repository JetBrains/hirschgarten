package org.jetbrains.bazel.sync.workspace.languages.protobuf

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
        .resolveBazelOutSymlinkInPath()
        .absolutePathString()
      val importPath = absoluteSourcePath.removePrefix(absoluteProtoSourceRoot)
        .removePrefix("/")
      SourceMapping(importPath, absoluteSourcePath)
    }
  }

  private fun Path.resolveBazelOutSymlinkInPath(): Path {
    try {
      return this.toRealPath()
    } catch (_: Throwable) {
      // continue with fallback logic
    }
    val pathString = this.toString()
    val bazelOutIndex = pathString.indexOf("bazel-out")

    if (bazelOutIndex == -1) {
      return this // no bazel-out in path
    }

    val beforeBazelOut = pathString.take(bazelOutIndex)
    val afterBazelOut = pathString.substring(bazelOutIndex)

    val bazelOutPath = Path.of(beforeBazelOut + "bazel-out")

    return if (Files.isSymbolicLink(bazelOutPath)) {
      val resolvedBazelOut = Files.readSymbolicLink(bazelOutPath)
      val basePath = Path.of(beforeBazelOut)
      basePath.resolve(resolvedBazelOut).resolve(afterBazelOut.removePrefix("bazel-out/"))
    } else {
      this // somehow bazel-out is not a symlink
    }
  }

}
