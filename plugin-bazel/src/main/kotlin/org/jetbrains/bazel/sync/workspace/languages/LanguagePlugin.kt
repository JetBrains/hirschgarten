package org.jetbrains.bazel.sync.workspace.languages

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetData
import java.nio.file.Path

interface LanguagePlugin<BuildTarget : BuildTargetData> {
  fun getSupportedLanguages(): Set<LanguageClass>

  fun resolveJvmPackagePrefix(source: Path): String? = null

  fun resolveExtraSources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> = emptySequence()

  fun resolveExtraResources(targetInfo: BspTargetInfo.TargetInfo): Sequence<Path> = emptySequence()

  fun onSync(targets: Sequence<BspTargetInfo.TargetInfo>, workspaceContext: WorkspaceContext) {}

  suspend fun createBuildTargetData(context: LanguagePluginContext, target: BspTargetInfo.TargetInfo): BuildTarget?
}
