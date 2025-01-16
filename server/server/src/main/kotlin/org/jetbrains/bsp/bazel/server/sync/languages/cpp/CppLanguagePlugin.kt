package org.jetbrains.bsp.bazel.server.sync.languages.cpp

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.CppOptionsItem
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.info.BspTargetInfo
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bsp.bazel.server.dependencygraph.DependencyGraph
import org.jetbrains.bsp.bazel.server.model.BspMappings
import org.jetbrains.bsp.bazel.server.model.Module
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.server.sync.languages.LanguagePlugin
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain.BazelCppToolchainResolver
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain.CompilerWrapper
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path

class CppLanguagePlugin(
  private val bazelPathsResolver: BazelPathsResolver,
  private val bazelRunner: BazelRunner,
  private val bazelCppToolchainResolver: BazelCppToolchainResolver = BazelCppToolchainResolver(bazelPathsResolver.bazelInfo, bazelRunner),
) : LanguagePlugin<CppModule>() {
  var cToolchainInfoLookupMap = mapOf<String, CToolchainInfo>()
  var targetsLookupMap = mapOf<String, TargetInfo>()

  val cppPathResolver = CppPathResolver(bazelPathsResolver)

  override fun prepareSync(targets: Sequence<TargetInfo>) {
    targetsLookupMap = targets.map { it.id to it }.toMap()
    cToolchainInfoLookupMap =
      targets
        .filter { it.hasCToolchainInfo() }
        .mapNotNull {
          val original = it.getCToolChainInfoOrNull() ?: return@mapNotNull null
          val oldCCompiler = cppPathResolver.resolveToPath(original.cCompiler)
          val oldCppCompiler = cppPathResolver.resolveToPath(original.cppCompiler)
          val newCCompiler =
            CompilerWrapper().createCompilerExecutableWrapper(
              Path.of(bazelPathsResolver.bazelInfo.execRoot),
              oldCCompiler,
              // todo: add xcode environment here
              mapOf(),
            )
          val newCppCompiler =
            CompilerWrapper().createCompilerExecutableWrapper(
              Path.of(bazelPathsResolver.bazelInfo.execRoot),
              oldCppCompiler,
              // todo: add xcode environment here
              mapOf(),
            )
          it.id to
            CToolchainInfo(
              builtInIncludeDirectory = original.builtInIncludeDirectoryList,
              cOptions = original.cOptionList,
              cppOptions = original.cppOptionList,
              cCompiler = bazelPathsResolver.resolveUri(newCCompiler).toString(),
              cppCompiler = bazelPathsResolver.resolveUri(newCppCompiler).toString(),
              compilerVersion = bazelCppToolchainResolver.getCompilerVersion(newCppCompiler, newCCompiler),
            )
        }.toMap()
  }

  override fun resolveModule(targetInfo: TargetInfo): CppModule? =
    targetInfo.getCppTargetInfoOrNull()?.run {
      // find the correct toolchain via the dependencies
      val toolchainInfo = targetInfo.dependenciesList.mapNotNull { cToolchainInfoLookupMap[Label.parse(it.id).toString()] }.firstOrNull()
      CppModule(
        copts = targetInfo.cppTargetInfo.coptsList,
        sources = targetInfo.sourcesList.map { bazelPathsResolver.resolveUri(it).toString() },
        headers = targetInfo.cppTargetInfo.headersList.map { bazelPathsResolver.resolveUri(it).toString() },
        textualHeaders = targetInfo.cppTargetInfo.textualHeadersList.map { bazelPathsResolver.resolveUri(it).toString() },
        transitiveIncludeDirectory =
          targetInfo.cppTargetInfo.transitiveIncludeDirectoryList
            .flatMap {
              cppPathResolver.resolveToIncludeDirectories(
                Path(it),
                targetsLookupMap,
              )
            }.map { it.toString().trimEnd('/') },
        transitiveQuoteIncludeDirectory =
          targetInfo.cppTargetInfo.transitiveQuoteIncludeDirectoryList
            .flatMap {
              cppPathResolver.resolveToIncludeDirectories(
                Path(it),
                targetsLookupMap,
              )
            }.map { it.toString().trimEnd('/') },
        transitiveSystemIncludeDirectory =
          targetInfo.cppTargetInfo.transitiveSystemIncludeDirectoryList
            .flatMap {
              cppPathResolver.resolveToIncludeDirectories(
                Path(it),
                targetsLookupMap,
              )
            }.map { it.toString().trimEnd('/') },
        transitiveDefine = transitiveDefineList,
        includePrefix = targetInfo.cppTargetInfo.includePrefix,
        stripIncludePrefix = targetInfo.cppTargetInfo.stripIncludePrefix,
        cToolchainInfo = toolchainInfo,
        execRoot = bazelPathsResolver.bazelInfo.execRoot,
      )
    }

  override fun applyModuleData(moduleData: CppModule, buildTarget: BuildTarget) {
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-612
    buildTarget.data = moduleData
    buildTarget.dataKind = BuildTargetDataKind.CPP
  }

  private fun TargetInfo.getCppTargetInfoOrNull(): BspTargetInfo.CppTargetInfo? = this.takeIf(TargetInfo::hasCppTargetInfo)?.cppTargetInfo

  private fun TargetInfo.getCToolChainInfoOrNull(): BspTargetInfo.CToolchainInfo? =
    this.takeIf(TargetInfo::hasCToolchainInfo)?.cToolchainInfo

  override fun dependencySources(targetInfo: TargetInfo, dependencyGraph: DependencyGraph): Set<URI> =
    targetInfo
      .getCppTargetInfoOrNull()
      ?.run {
        dependencyGraph
          .transitiveDependenciesWithoutRootTargets(Label.parse(targetInfo.id))
          .flatMap(TargetInfo::getSourcesList)
          .map(bazelPathsResolver::resolveUri)
          .toSet()
      }.orEmpty()

  fun toCppOptionsItem(module: Module, cppModule: CppModule): CppOptionsItem =
    CppOptionsItem(
      BspMappings.toBspId(module),
      cppModule.copts,
      listOf(),
      listOf(),
    )
}
