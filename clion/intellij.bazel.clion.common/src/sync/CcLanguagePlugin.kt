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
import org.jetbrains.bsp.protocol.OutputLocationParser
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
      target.hasCIdeInfo() -> listOf(mapIdeInfo(target.cIdeInfo, server.outputParser))
      target.hasCToolchainIdeInfo() -> listOf(mapToolchainIdeInfo(target.cToolchainIdeInfo, server.outputParser))
      else -> emptyList()
    }
  }
}

private suspend fun mapIdeInfo(info: CIdeInfo, outputParser: OutputLocationParser): CcBuildTarget {
  return CcBuildTarget(
    ruleContext = if (info.hasRuleContext()) mapRuleContext(info.ruleContext, outputParser) else null,
    compilationContext = mapCompilationContext(info.compilationContext, outputParser),
  )
}

private suspend fun mapRuleContext(ctx: CIdeInfo.RuleContext, outputParser: OutputLocationParser): CcBuildTarget.RuleContext {
  return CcBuildTarget.RuleContext(
    headers = OutputLocationCollectionBuilder.build(ctx.headersList, outputParser),
    textualHeaders = OutputLocationCollectionBuilder.build(ctx.textualHeadersList, outputParser),
    copts = ctx.coptsList.toList(),
    conlyopts = ctx.conlyoptsList.toList(),
    cxxopts = ctx.cxxoptsList.toList(),
    args = ctx.argsList.toList(),
    includePrefix = ctx.includePrefix,
    stripIncludePrefix = ctx.stripIncludePrefix,
  )
}

private suspend fun mapCompilationContext(
  ctx: CIdeInfo.CompilationContext,
  outputParser: OutputLocationParser,
): CcBuildTarget.CompilationContext {
  return CcBuildTarget.CompilationContext(
    headers = OutputLocationCollectionBuilder.build(ctx.headersList, outputParser),
    defines = ctx.definesList.toList(),
    includes = OutputLocationCollectionBuilder.buildExecroot(ctx.includesList, outputParser),
    quoteIncludes = OutputLocationCollectionBuilder.buildExecroot(ctx.quoteIncludesList, outputParser),
    systemIncludes = OutputLocationCollectionBuilder.buildExecroot(ctx.systemIncludesList, outputParser),
  )
}

private suspend fun mapToolchainIdeInfo(info: IntellijIdeInfo.CToolchainIdeInfo, parser: OutputLocationParser): CcToolchainBuildTarget {
  return CcToolchainBuildTarget(
    targetName = info.targetName,
    compilerName = info.compilerName,
    cppOption = info.cppOptionList.toList(),
    cOption = info.cOptionList.toList(),
    cCompiler = parser.parseExecrootPath(info.cCompiler),
    cppCompiler = parser.parseExecrootPath(info.cppCompiler),
    builtInIncludeDirectories = OutputLocationCollectionBuilder.buildExecroot(info.builtInIncludeDirectoryList, parser),
    sysroot = info.sysroot.takeIf { it.isNotEmpty() }?.let { parser.parseExecrootPath(it) },
    cEnvironment = info.cEnvironmentMap.toMap(),
    cppEnvironment = info.cppEnvironmentMap.toMap(),
  )
}

