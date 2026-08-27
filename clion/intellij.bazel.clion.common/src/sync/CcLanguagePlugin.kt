package org.jetbrains.bazel.clion.sync

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.CIdeInfo
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.OutputLocation
import kotlin.reflect.KClass

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

  override suspend fun mapBuildTargetData(
    server: BazelServerFacade,
    target: IntellijIdeInfo.TargetIdeInfo,
    repoMapping: RepoMapping,
  ): List<BuildTargetData> {
    return when {
      target.hasCIdeInfo() -> listOf(mapIdeInfo(target.cIdeInfo))
      target.hasCToolchainIdeInfo() -> listOf(mapToolchainIdeInfo(target.cToolchainIdeInfo))
      else -> emptyList()
    }
  }
}

private fun mapIdeInfo(info: CIdeInfo): CcBuildTarget {
  return CcBuildTarget(
    ruleContext = if (info.hasRuleContext()) mapRuleContext(info.ruleContext) else null,
    compilationContext = mapCompilationContext(info.compilationContext),
  )
}

private fun mapRuleContext(ctx: CIdeInfo.RuleContext): CcBuildTarget.RuleContext {
  return CcBuildTarget.RuleContext(
    headers = OutputLocationCollectionBuilder.build(ctx.headersList),
    textualHeaders = OutputLocationCollectionBuilder.build(ctx.textualHeadersList),
    copts = ctx.coptsList.toList(),
    conlyopts = ctx.conlyoptsList.toList(),
    cxxopts = ctx.cxxoptsList.toList(),
    args = ctx.argsList.toList(),
    includePrefix = ctx.includePrefix,
    stripIncludePrefix = ctx.stripIncludePrefix,
  )
}

private fun mapCompilationContext(ctx: CIdeInfo.CompilationContext): CcBuildTarget.CompilationContext {
  return CcBuildTarget.CompilationContext(
    headers = OutputLocationCollectionBuilder.build(ctx.headersList),
    defines = ctx.definesList.toList(),
    includes = OutputLocationCollectionBuilder.buildExecroot(ctx.includesList),
    quoteIncludes = OutputLocationCollectionBuilder.buildExecroot(ctx.quoteIncludesList),
    systemIncludes = OutputLocationCollectionBuilder.buildExecroot(ctx.systemIncludesList),
  )
}

private fun mapToolchainIdeInfo(info: IntellijIdeInfo.CToolchainIdeInfo): CcToolchainBuildTarget {
  return CcToolchainBuildTarget(
    targetName = info.targetName,
    compilerName = info.compilerName,
    cppOption = info.cppOptionList.toList(),
    cOption = info.cOptionList.toList(),
    cCompiler = OutputLocation.parseExecrootPath(info.cCompiler),
    cppCompiler = OutputLocation.parseExecrootPath(info.cppCompiler),
    builtInIncludeDirectories = OutputLocationCollectionBuilder.buildExecroot(info.builtInIncludeDirectoryList),
    sysroot = info.sysroot.takeIf { it.isNotEmpty() }?.let(OutputLocation::parseExecrootPath),
    cEnvironment = info.cEnvironmentMap.toMap(),
    cppEnvironment = info.cppEnvironmentMap.toMap(),
  )
}

