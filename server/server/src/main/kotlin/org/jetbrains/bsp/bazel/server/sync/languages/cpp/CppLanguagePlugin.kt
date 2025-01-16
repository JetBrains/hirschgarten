package org.jetbrains.bsp.bazel.server.sync.languages.cpp

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.CppOptionsItem
import org.apache.logging.log4j.LogManager
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
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain.XCodeCompilerSettingProvider
import org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain.XCodeCompilerSettingProviderImpl
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path

class CppLanguagePlugin(
  private val bazelPathsResolver: BazelPathsResolver,
  private val bazelRunner: BazelRunner,
  private val bazelCppToolchainResolver: BazelCppToolchainResolver
) : LanguagePlugin<CppModule>() {
  private val log = LogManager.getLogger(CppLanguagePlugin::class.java)

  var cToolchainInfoLookupMap = mapOf<String, CToolchainInfo>()
  var targetsLookupMap = mapOf<String, TargetInfo>()

  val cppPathResolver = CppPathResolver(bazelPathsResolver.bazelInfo)
  val xCodeCompilerSettingProvider: XCodeCompilerSettingProvider = XCodeCompilerSettingProviderImpl()

  // BazelCppToolchainResolver needs to be mocked if we want to test
  // so CppLanguagePlugin has a second constructor
  constructor(bazelPathsResolver: BazelPathsResolver, bazelRunner: BazelRunner) : this(
    bazelPathsResolver,
    bazelRunner,
    BazelCppToolchainResolver(bazelPathsResolver.bazelInfo, bazelRunner),
  )


  override fun prepareSync(targets: Sequence<TargetInfo>) {
    targetsLookupMap = targets.map { it.id to it }.toMap()
    val res = xCodeCompilerSettingProvider.fromContext(bazelRunner)?.asEnvironmentVariables()
    log.info("Environment variable from Xcode: $res")
    cToolchainInfoLookupMap = targets.filter { it.hasCToolchainInfo() }.map {
      val original = it.getCToolChainInfoOrNull()!!
      val oldCCompiler = bazelPathsResolver.resolve(original.cCompiler)
      val oldCppCompiler = bazelPathsResolver.resolve(original.cppCompiler)
      val newCCompiler = CompilerWrapper().createCompilerExecutableWrapper(
        Path.of(bazelPathsResolver.bazelInfo.execRoot), oldCCompiler,
        res ?: mapOf(),
      )
      val newCppCompiler = CompilerWrapper().createCompilerExecutableWrapper(
        Path.of(bazelPathsResolver.bazelInfo.execRoot), oldCppCompiler,
        res ?: mapOf(),
      )
      it.id to CToolchainInfo(
        builtInIncludeDirectory = original.builtInIncludeDirectoryList,
        cOptions = original.cOptionList,
        cppOptions = original.cppOptionList,
        cCompiler = bazelPathsResolver.resolveUri(newCCompiler).toString(),
        cppCompiler = bazelPathsResolver.resolveUri(newCppCompiler).toString(),
        compilerVersion = bazelCppToolchainResolver.getCompilerVersion(newCppCompiler.toString(), newCCompiler.toString()),
      )
    }.toMap()
    log.info("Detected CPP Toolchains: $cToolchainInfoLookupMap")
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
        transitiveIncludeDirectory = targetInfo.cppTargetInfo.transitiveIncludeDirectoryList.flatMap {
          cppPathResolver.resolveToIncludeDirectories(
            Path(it), targetsLookupMap,
          )
        }.map { it.toString().trimEnd('/') },
        transitiveQuoteIncludeDirectory = targetInfo.cppTargetInfo.transitiveQuoteIncludeDirectoryList.flatMap {
          cppPathResolver.resolveToIncludeDirectories(
            Path(it), targetsLookupMap,
          )
        }.map { it.toString().trimEnd('/') },
        transitiveSystemIncludeDirectory = targetInfo.cppTargetInfo.transitiveSystemIncludeDirectoryList.flatMap {
          cppPathResolver.resolveToIncludeDirectories(
            Path(it), targetsLookupMap,
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
    log.info("Built target Detected in CPPLanguagePlugin: ${buildTarget.dataKind}")
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
