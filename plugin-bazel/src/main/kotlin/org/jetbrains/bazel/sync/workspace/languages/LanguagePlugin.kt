package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.sync.workspace.model.LanguageData
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetData
import java.nio.file.Path

// TODO: merge createIntermediateModel -> createBuildTargetData
interface LanguagePlugin<INTERMEDIATE : LanguageData, BUILD_TARGET : BuildTargetData> {
  fun getSupportedLanguages(): Set<LanguageClass>

  fun calculateJvmPackagePrefix(source: Path): String? = null

  fun calculateAdditionalSources(targetInfo: BspTargetInfo.TargetInfo): List<BspTargetInfo.FileLocation> = listOf()

  fun resolveAdditionalResources(targetInfo: BspTargetInfo.TargetInfo): Set<Path> = emptySet()

  fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {}

  fun createIntermediateModel(targetInfo: BspTargetInfo.TargetInfo): INTERMEDIATE?

  fun createBuildTargetData(context: LanguagePluginContext, ir: INTERMEDIATE): BUILD_TARGET?
}

@Suppress("UNCHECKED_CAST")
fun <IR : LanguageData, BT : BuildTargetData> LanguagePlugin<IR, BT>.createBuildDataUnsafe(context: LanguagePluginContext, ir: Any): BT? =
  this.createBuildTargetData(context, ir as IR)
