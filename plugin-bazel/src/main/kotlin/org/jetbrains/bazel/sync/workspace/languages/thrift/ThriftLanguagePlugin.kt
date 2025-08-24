package org.jetbrains.bazel.sync.workspace.languages.thrift

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync.workspace.graph.DependencyGraph
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

class ThriftLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<ThriftModule>() {
  override fun dependencySources(targetInfo: BspTargetInfo.TargetInfo, dependencyGraph: DependencyGraph): Set<Path> {
    val transitiveSourceDeps =
      dependencyGraph
        .transitiveDependenciesWithoutRootTargets(targetInfo.label())
        .filter(::isThriftLibrary)
        .flatMap(BspTargetInfo.TargetInfo::getSourcesList)
        .map(bazelPathsResolver::resolve)
        .toHashSet()

    val directSourceDeps = sourcesFromJvmTargetInfo(targetInfo)

    return transitiveSourceDeps + directSourceDeps
  }

  private fun sourcesFromJvmTargetInfo(targetInfo: BspTargetInfo.TargetInfo): HashSet<Path> =
    if (targetInfo.hasJvmTargetInfo()) {
      targetInfo
        .jvmTargetInfo
        .jarsList
        .flatMap { it.sourceJarsList }
        .map(bazelPathsResolver::resolve)
        .toHashSet()
    } else {
      HashSet()
    }

  private fun isThriftLibrary(target: BspTargetInfo.TargetInfo): Boolean = target.kind == THRIFT_LIBRARY_RULE_NAME

  override fun applyModuleData(moduleData: ThriftModule, buildTarget: RawBuildTarget) {}

  companion object {
    private const val THRIFT_LIBRARY_RULE_NAME = "thrift_library"
  }
}
