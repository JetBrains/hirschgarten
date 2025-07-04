package org.jetbrains.bazel.server.sync.languages

import org.jetbrains.bazel.info.FileLocation
import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.model.LanguageData
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

abstract class LanguagePlugin<T : LanguageData> {
  open fun calculateJvmPackagePrefix(source: Path): String? = null

  open fun calculateAdditionalSources(targetInfo: TargetInfo): List<FileLocation> = listOf()

  open fun resolveAdditionalResources(targetInfo: TargetInfo): Set<Path> = emptySet()

  open fun prepareSync(targets: Sequence<TargetInfo>, workspaceContext: WorkspaceContext) {}

  open fun resolveModule(targetInfo: TargetInfo): T? = null

  open fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<Path> = emptySet()

  @Suppress("UNCHECKED_CAST")
  fun setModuleData(moduleData: LanguageData, buildTarget: RawBuildTarget) = applyModuleData(moduleData as T, buildTarget)

  protected abstract fun applyModuleData(moduleData: T, buildTarget: RawBuildTarget)
}
