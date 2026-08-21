package org.jetbrains.bazel.clion.sync

import com.google.devtools.intellij.aspect.Common
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CIdeInfo
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bsp.protocol.BuildTargetData
import kotlin.reflect.KClass

private typealias ArtifactResolver = (Common.ArtifactLocation) -> ArtifactLocation

@ApiStatus.Internal
class CcLanguagePlugin : LanguagePlugin {

  override val providedBuildTargetTypes: Set<KClass<out BuildTargetData>>
    get() = setOf(CcBuildTarget::class, CcToolchainBuildTarget::class)

  override fun getSupportedLanguages(): Set<LanguageClass> = setOf(CC_LANGUAGE_CLASS)

  override fun collectUsedLanguages(target: IntellijIdeInfo.TargetIdeInfo): List<LanguageClass> {
    return if (target.hasCIdeInfo() || target.hasCToolchainIdeInfo()) {
      listOf(CC_LANGUAGE_CLASS)
    }
    else {
      emptyList()
    }
  }

  private fun captureArtifactResolver(server: BazelServerFacade, repoMapping: RepoMapping): ArtifactResolver {
    val localRepositories = repoMapping.getLocalRepositories()

    return { location ->
      ArtifactLocation(
        rootPath = location.rootPath,
        relativePath = location.relativePath,
        isSource = location.isSource,
        isExternal = server.bazelPathsResolver.isExternal(location, localRepositories),
        resolvedPath = server.bazelPathsResolver.resolve(location, localRepositories),
      )
    }
  }

  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: IntellijIdeInfo.TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    val resolver = captureArtifactResolver(server, repoMapping)

    return when {
      target.hasCIdeInfo() -> listOf(mapIdeInfo(target.cIdeInfo, resolver))
      target.hasCToolchainIdeInfo() -> listOf(mapToolchainIdeInfo(target.cToolchainIdeInfo))
      else -> emptyList()
    }
  }
}

private fun mapIdeInfo(info: CIdeInfo, resolver: ArtifactResolver): CcBuildTarget {
  return CcBuildTarget(
    ruleContext = if (info.hasRuleContext()) mapRuleContext(info.ruleContext, resolver) else null,
    compilationContext = mapCompilationContext(info.compilationContext, resolver),
  )
}

private fun mapRuleContext(ctx: CIdeInfo.RuleContext, resolver: ArtifactResolver): CcBuildTarget.RuleContext {
  return CcBuildTarget.RuleContext(
    headers = ctx.headersList.map(resolver),
    textualHeaders = ctx.textualHeadersList.map(resolver),
    copts = ctx.coptsList.toList(),
    conlyopts = ctx.conlyoptsList.toList(),
    cxxopts = ctx.cxxoptsList.toList(),
    args = ctx.argsList.toList(),
    includePrefix = ctx.includePrefix,
    stripIncludePrefix = ctx.stripIncludePrefix,
  )
}

private fun mapCompilationContext(ctx: CIdeInfo.CompilationContext, resolver: ArtifactResolver): CcBuildTarget.CompilationContext {
  return CcBuildTarget.CompilationContext(
    headers = ctx.headersList.map(resolver),
    defines = ctx.definesList.toList(),
    includes = ctx.includesList.map(::ExecutionRootPath),
    quoteIncludes = ctx.quoteIncludesList.map(::ExecutionRootPath),
    systemIncludes = ctx.systemIncludesList.map(::ExecutionRootPath),
  )
}

private fun mapToolchainIdeInfo(info: IntellijIdeInfo.CToolchainIdeInfo): CcToolchainBuildTarget {
  return CcToolchainBuildTarget(
    targetName = info.targetName,
    compilerName = info.compilerName,
    cppOption = info.cppOptionList.toList(),
    cOption = info.cOptionList.toList(),
    cCompiler = ExecutionRootPath(info.cCompiler),
    cppCompiler = ExecutionRootPath(info.cppCompiler),
    builtInIncludeDirectories = info.builtInIncludeDirectoryList.map(::ExecutionRootPath),
    sysroot = ExecutionRootPath(info.sysroot),
    cEnvironment = info.cEnvironmentMap.toMap(),
    cppEnvironment = info.cppEnvironmentMap.toMap(),
  )
}

