package org.jetbrains.bsp.bazel.server.sync.languages

import ch.epfl.scala.bsp4j.BuildTarget
import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bsp.bazel.server.model.LanguageData
import org.jetbrains.bsp.protocol.EnhancedSourceItemData
import java.net.URI
import java.nio.file.Path

data class SourceRootAndData(val sourceRoot: Path, val data: EnhancedSourceItemData? = null)

abstract class LanguagePlugin<T : LanguageData> {
  open fun calculateSourceRootAndAdditionalData(source: Path): SourceRootAndData? = null

  open fun calculateAdditionalSources(targetInfo: BspTargetInfo.TargetInfo): List<BspTargetInfo.FileLocation> = listOf()

  open fun resolveAdditionalResources(targetInfo: BspTargetInfo.TargetInfo): Set<URI> = emptySet()

  open fun prepareSync(targets: Sequence<BspTargetInfo.TargetInfo>) {}

  open fun resolveModule(targetInfo: BspTargetInfo.TargetInfo): T? = null

  open fun dependencySources(targetInfo: BspTargetInfo.TargetInfo, dependencyGraph: DependencyGraph): Set<URI> = emptySet()

  @Suppress("UNCHECKED_CAST")
  fun setModuleData(moduleData: LanguageData, buildTarget: BuildTarget) = applyModuleData(moduleData as T, buildTarget)

  protected abstract fun applyModuleData(moduleData: T, buildTarget: BuildTarget)
}
