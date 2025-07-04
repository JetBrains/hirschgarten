package org.jetbrains.bazel.server.sync.languages.cpp

import org.jetbrains.bazel.info.CppTargetInfo
import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.CppBuildTarget
import org.jetbrains.bsp.protocol.CppOptionsItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

class CppLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<CppModule>() {
  override fun resolveModule(targetInfo: TargetInfo): CppModule? = null

  override fun applyModuleData(moduleData: CppModule, buildTarget: RawBuildTarget) {
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-612
    val cppBuildTarget =
      CppBuildTarget(
        version = null,
        compiler = "compiler",
        cCompiler = "/bin/gcc",
        cppCompiler = "/bin/gcc",
      )
    buildTarget.data = cppBuildTarget
  }

  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<Path> =
    targetInfo.cppTargetInfo
      ?.run {
        dependencyGraph
          .transitiveDependenciesWithoutRootTargets(targetInfo.id)
          .flatMap(TargetInfo::sources)
          .map(bazelPathsResolver::resolve)
          .toSet()
      }.orEmpty()

  fun toCppOptionsItem(module: Module, cppModule: CppModule): CppOptionsItem =
    CppOptionsItem(
      module.label,
      cppModule.copts,
      cppModule.defines,
      cppModule.linkOpts,
    )
}
