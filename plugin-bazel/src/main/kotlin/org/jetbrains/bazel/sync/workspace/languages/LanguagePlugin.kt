package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetData
import java.nio.file.Path

interface LanguagePlugin<BuildTarget : BuildTargetData> {
  fun getSupportedLanguages(): Set<LanguageClass>

  fun calculateJvmPackagePrefix(source: Path): String? = null

  fun calculateAdditionalSources(targetInfo: BspTargetInfo.TargetInfo): List<BspTargetInfo.FileLocation> = listOf()

  fun resolveAdditionalResources(targetInfo: BspTargetInfo.TargetInfo): Set<Path> = emptySet()

  fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {}

  suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): BuildTarget?
}
