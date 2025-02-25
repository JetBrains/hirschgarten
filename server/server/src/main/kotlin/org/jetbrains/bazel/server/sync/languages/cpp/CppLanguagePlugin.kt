package org.jetbrains.bazel.server.sync.languages.cpp

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.server.model.BspMappings
import org.jetbrains.bazel.server.model.Module
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.CppBuildTarget
import org.jetbrains.bsp.protocol.CppOptionsItem
import java.net.URI

class CppLanguagePlugin(private val bazelPathsResolver: BazelPathsResolver) : LanguagePlugin<CppModule>() {
  override fun resolveModule(targetInfo: TargetInfo): CppModule? = null

  override fun applyModuleData(moduleData: CppModule, buildTarget: BuildTarget) {
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

  private fun TargetInfo.getCppTargetInfoOrNull(): BspTargetInfo.CppTargetInfo? = this.takeIf(TargetInfo::hasCppTargetInfo)?.cppTargetInfo

  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<URI> =
    targetInfo
      .getCppTargetInfoOrNull()
      ?.run {
        dependencyGraph
          .transitiveDependenciesWithoutRootTargets(targetInfo.label())
          .flatMap(TargetInfo::getSourcesList)
          .map(bazelPathsResolver::resolveUri)
          .toSet()
      }.orEmpty()

  fun toCppOptionsItem(module: Module, cppModule: CppModule): CppOptionsItem =
    CppOptionsItem(
      BspMappings.toBspId(module),
      cppModule.copts,
      cppModule.defines,
      cppModule.linkOpts,
    )
}
