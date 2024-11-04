package org.jetbrains.bsp.bazel.server.bsp.managers

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.aspect
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.buildManualTests
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.buildTagFilters
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.color
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.curses
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.keepGoing
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.outputGroups
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.bazel.server.bep.BepOutput
import org.jetbrains.bsp.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Paths

data class BazelBspAspectsManagerResult(val bepOutput: BepOutput, val isFailure: Boolean)

data class RuleLanguage(val ruleName: String?, val language: Language)

class BazelBspAspectsManager(
  private val bazelBspCompilationManager: BazelBspCompilationManager,
  private val aspectsResolver: InternalAspectsResolver,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val featureFlags: FeatureFlags,
) {
  private val aspectsPath = Paths.get(aspectsResolver.bazelBspRoot, Constants.ASPECTS_ROOT)
  private val templateWriter = TemplateWriter(aspectsPath)

  fun calculateRuleLanguages(externalRuleNames: List<String>): List<RuleLanguage> =
    Language
      .values()
      .mapNotNull { language ->
        if (language.ruleNames.isEmpty()) return@mapNotNull RuleLanguage(null, language) // bundled in Bazel
        val ruleName = language.ruleNames.firstOrNull { externalRuleNames.contains(it) }
        ruleName?.let { RuleLanguage(it, language) }
      }.removeDisabledLanguages()
      .addNativeAndroidLanguageIfNeeded()

  private fun List<RuleLanguage>.removeDisabledLanguages(): List<RuleLanguage> {
    val disabledLanguages =
      buildSet {
        if (!featureFlags.isAndroidSupportEnabled) add(Language.Android)
        if (!featureFlags.isGoSupportEnabled) add(Language.Go)
        if (!featureFlags.isRustSupportEnabled) add(Language.Rust)
      }
    return filterNot { it.language in disabledLanguages }
  }

  private fun List<RuleLanguage>.addNativeAndroidLanguageIfNeeded(): List<RuleLanguage> {
    if (!featureFlags.isAndroidSupportEnabled) return this
    if (!workspaceContextProvider.currentWorkspaceContext().enableNativeAndroidRules.value) return this
    return this.filterNot { it.language == Language.Android } + RuleLanguage(null, Language.Android)
  }

  fun generateAspectsFromTemplates(
    ruleLanguages: List<RuleLanguage>,
    workspaceContext: WorkspaceContext,
    toolchains: Map<RuleLanguage, String?>,
  ) {
    ruleLanguages.filter { it.language.isTemplate }.forEach {
      val outputFile = aspectsPath.resolve(it.language.toAspectRelativePath())
      val templateFilePath = it.language.toAspectTemplateRelativePath()
      val kotlinEnabled = Language.Kotlin in ruleLanguages.map { it.language }
      val variableMap =
        mapOf(
          "ruleName" to it.ruleName,
          "addTransitiveCompileTimeJars" to
            workspaceContext.experimentalAddTransitiveCompileTimeJars.value.toStarlarkString(),
          "loadKtJvmProvider" to
            if (kotlinEnabled) """load("//aspects:rules/kt/kt_info.bzl", "get_kt_jvm_provider")""" else "",
          "getKtJvmProvider" to
            if (kotlinEnabled) "get_kt_jvm_provider(target)" else "None",
          "toolchainType" to toolchains[it].orEmpty(),
        )
      templateWriter.writeToFile(templateFilePath, outputFile, variableMap)
    }

    templateWriter.writeToFile(
      Constants.CORE_BZL + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve(Constants.CORE_BZL),
      mapOf("isPropagateExportsFromDepsEnabled" to featureFlags.isPropagateExportsFromDepsEnabled.toStarlarkString()),
    )
  }

  private fun Boolean.toStarlarkString(): String = if (this) "True" else "False"

  fun fetchFilesFromOutputGroups(
    cancelChecker: CancelChecker,
    targetSpecs: TargetsSpec,
    aspect: String,
    outputGroups: List<String>,
    shouldSyncManualFlags: Boolean,
    isRustEnabled: Boolean,
  ): BazelBspAspectsManagerResult {
    if (targetSpecs.values.isEmpty()) return BazelBspAspectsManagerResult(BepOutput(), isFailure = false)
    val defaultFlags =
      listOf(
        aspect(aspectsResolver.resolveLabel(aspect)),
        outputGroups(outputGroups),
        keepGoing(),
        color(true),
        curses(false),
        buildTagFilters(listOf("-no-ide")),
      )
    val allowManualTargetsSyncFlags = if (shouldSyncManualFlags) listOf(buildManualTests()) else emptyList()

    val flagsToUse = defaultFlags + allowManualTargetsSyncFlags

    return bazelBspCompilationManager
      .buildTargetsWithBep(
        cancelChecker = cancelChecker,
        targetSpecs = targetSpecs,
        extraFlags = flagsToUse,
        originId = null,
        // Setting `CARGO_BAZEL_REPIN=1` updates `cargo_lockfile`
        // (`Cargo.lock` file) based on dependencies specified in `manifest`
        // (`Cargo.toml` file) and syncs `lockfile` (`Cargo.bazel.lock` file) with `cargo_lockfile`.
        // Ensures that both Bazel and Cargo are using the same versions of dependencies.
        // Mentioned `cargo_lockfile`, `lockfile` and `manifest` are defined in
        // `crates_repository` from `rules_rust`,
        // see: https://bazelbuild.github.io/rules_rust/crate_universe.html#crates_repository.
        // In our server used only with `bazel build` command.
        environment = if (isRustEnabled) listOf(Pair("CARGO_BAZEL_REPIN", "1")) else emptyList(),
      ).let {
        BazelBspAspectsManagerResult(it.bepOutput, it.processResult.isNotSuccess)
      }
  }
}
