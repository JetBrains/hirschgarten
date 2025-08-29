package org.jetbrains.bazel.sync.workspace.languages.thrift

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginContext
import org.jetbrains.bsp.protocol.VoidBuildTarget

class ThriftLanguagePlugin : LanguagePlugin<VoidBuildTarget> {
  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(LanguageClass.THRIFT)

  override suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): VoidBuildTarget =
    VoidBuildTarget
}
