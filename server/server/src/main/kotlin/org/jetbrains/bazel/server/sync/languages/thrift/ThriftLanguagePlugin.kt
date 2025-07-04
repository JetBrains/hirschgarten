package org.jetbrains.bazel.server.sync.languages.thrift

import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

class ThriftLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<ThriftModule>() {
  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<Path> {
    val transitiveSourceDeps =
      dependencyGraph
        .transitiveDependenciesWithoutRootTargets(targetInfo.id)
        .filter(::isThriftLibrary)
        .flatMap(TargetInfo::sources)
        .map(bazelPathsResolver::resolve)
        .toHashSet()

    val directSourceDeps = sourcesFromJvmTargetInfo(targetInfo)

    return transitiveSourceDeps + directSourceDeps
  }

  private fun sourcesFromJvmTargetInfo(targetInfo: TargetInfo): HashSet<Path> =
      targetInfo
        .jvmTargetInfo
        ?.jars
        ?.flatMap { it.sourceJars}
        ?.map(bazelPathsResolver::resolve)
        ?.toHashSet()
        ?: hashSetOf()

  private fun isThriftLibrary(target: TargetInfo): Boolean = target.kind == THRIFT_LIBRARY_RULE_NAME

  override fun applyModuleData(moduleData: ThriftModule, buildTarget: RawBuildTarget) {}

  companion object {
    private const val THRIFT_LIBRARY_RULE_NAME = "thrift_library"
  }
}
