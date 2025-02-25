package org.jetbrains.bsp.bazel.server.bsp.managers

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.aspect
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.buildManualTests
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.buildTagFilters
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.color
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.curses
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.keepGoing
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.outputGroups
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.commons.BazelStatus
import org.jetbrains.bsp.bazel.server.bep.BepOutput
import org.jetbrains.bsp.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bsp.bazel.server.bzlmod.BzlmodRepoMapping
import org.jetbrains.bsp.bazel.server.bzlmod.RepoMapping
import org.jetbrains.bsp.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import java.nio.file.Paths

data class BazelBspAspectsManagerResult(val bepOutput: BepOutput, val status: BazelStatus) {
  val isFailure: Boolean
    get() = status != BazelStatus.SUCCESS

  fun merge(anotherResult: BazelBspAspectsManagerResult): BazelBspAspectsManagerResult =
    BazelBspAspectsManagerResult(bepOutput.merge(anotherResult.bepOutput), status.merge(anotherResult.status))

  companion object {
    fun emptyResult(): BazelBspAspectsManagerResult = BazelBspAspectsManagerResult(BepOutput(), BazelStatus.SUCCESS)
  }
}

data class RulesetLanguage(val rulesetName: String?, val language: Language)

class BazelBspAspectsManager(
  private val bazelBspCompilationManager: BazelBspCompilationManager,
  private val aspectsResolver: InternalAspectsResolver,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bazelRelease: BazelRelease,
) {
  private val aspectsPath = Paths.get(aspectsResolver.bazelBspRoot, Constants.ASPECTS_ROOT)
  private val templateWriter = TemplateWriter(aspectsPath)

  fun calculateRulesetLanguages(externalRulesetNames: List<String>): List<RulesetLanguage> =
    Language
      .entries
      .mapNotNull { language ->
        if (language.isBundled && bazelRelease.major < 8) return@mapNotNull RulesetLanguage(null, language) // bundled in Bazel version < 8
        val rulesetName = language.rulesetNames.firstOrNull { externalRulesetNames.contains(it) }
        rulesetName?.let { RulesetLanguage(it, language) }
      }.removeDisabledLanguages()
      .addNativeAndroidLanguageIfNeeded()
      .addExternalPythonLanguageIfNeeded(externalRulesetNames)

  private fun List<RulesetLanguage>.removeDisabledLanguages(): List<RulesetLanguage> {
    val disabledLanguages =
      buildSet {
        if (!BspFeatureFlags.isAndroidSupportEnabled) add(Language.Android)
        if (!BspFeatureFlags.isGoSupportEnabled) add(Language.Go)
        if (!BspFeatureFlags.isRustSupportEnabled) add(Language.Rust)
        if (!BspFeatureFlags.isCppSupportEnabled) add(Language.Cpp)
      }
    return filterNot { it.language in disabledLanguages }
  }

  private fun List<RulesetLanguage>.addNativeAndroidLanguageIfNeeded(): List<RulesetLanguage> {
    if (!BspFeatureFlags.isAndroidSupportEnabled) return this
    if (!workspaceContextProvider.currentWorkspaceContext().enableNativeAndroidRules.value) return this
    return this.filterNot { it.language == Language.Android } + RulesetLanguage(null, Language.Android)
  }

  private fun List<RulesetLanguage>.addExternalPythonLanguageIfNeeded(externalRulesetNames: List<String>): List<RulesetLanguage> {
    val rulesetName = Language.Python.rulesetNames.firstOrNull { externalRulesetNames.contains(it) }
    return this.filterNot { it.language == Language.Python } + RulesetLanguage(rulesetName, Language.Python)
  }

  fun generateAspectsFromTemplates(
    rulesetLanguages: List<RulesetLanguage>,
    workspaceContext: WorkspaceContext,
    toolchains: Map<RulesetLanguage, Label?>,
    bazelRelease: BazelRelease,
    repoMapping: RepoMapping,
  ) {
    val languageRuleMap = rulesetLanguages.associateBy { it.language }
    val activeLanguages = rulesetLanguages.map { it.language }.toSet()
    val kotlinEnabled = Language.Kotlin in activeLanguages
    val cppEnabled = Language.Cpp in activeLanguages
    val javaEnabled = Language.Java in activeLanguages
    val pythonEnabled = Language.Python in activeLanguages
    val bazel8OrAbove = bazelRelease.major >= 8
    Language.entries.filter { it.isTemplate }.forEach {
      val ruleLanguage = languageRuleMap[it]

      val outputFile = aspectsPath.resolve(it.toAspectRelativePath())
      val templateFilePath = it.toAspectTemplateRelativePath()
      val variableMap =
        mapOf(
          "rulesetName" to ruleLanguage?.calculateCanonicalName(repoMapping),
          "addTransitiveCompileTimeJars" to
            workspaceContext.experimentalAddTransitiveCompileTimeJars.value.toStarlarkString(),
          "transitiveCompileTimeJarsTargetKinds" to
            workspaceContext.experimentalTransitiveCompileTimeJarsTargetKinds.values.toStarlarkString(),
          "kotlinEnabled" to kotlinEnabled.toString(),
          "javaEnabled" to javaEnabled.toString(),
          "pythonEnabled" to pythonEnabled.toString(),
          "bazel8OrAbove" to bazel8OrAbove.toString(),
          "toolchainType" to ruleLanguage?.let { rl -> toolchains[rl]?.toString()?.let { "\"" + it + "\"" } },
        )
      templateWriter.writeToFile(templateFilePath, outputFile, variableMap)
    }

    templateWriter.writeToFile(
      Constants.CORE_BZL + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve(Constants.CORE_BZL),
      mapOf(
        "isPropagateExportsFromDepsEnabled" to BspFeatureFlags.isPropagateExportsFromDepsEnabled.toStarlarkString(),
      ),
    )

    // https://bazel.build/rules/lib/builtins/Label#repo_name
    // The canonical name of the repository containing the target referred to by this label, without any leading at-signs (@).
    val starlarkRepoMapping =
      when (repoMapping) {
        is BzlmodRepoMapping -> {
          repoMapping.canonicalRepoNameToLocalPath
            .map { (key, value) ->
              "\"${key.dropWhile { it == '@' }}\": \"$value\""
            }.joinToString(",\n", "{\n", "\n}")
        }

        is RepoMappingDisabled -> "{}"
      }

    templateWriter.writeToFile(
      "utils/utils.bzl" + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve("utils").resolve("utils.bzl"),
      mapOf(
        "repoMapping" to starlarkRepoMapping,
        "cppDeps" to if (cppEnabled) "\"_cc_toolchain\"," else "",
      ),
    )
  }

  private fun RulesetLanguage.calculateCanonicalName(repoMapping: RepoMapping): String? =
    when {
      // bazel mod dump_repo_mapping returns everything without @@
      // and in aspects we have a @ prefix
      repoMapping is BzlmodRepoMapping && rulesetName != null ->
        repoMapping.apparentRepoNameToCanonicalName[rulesetName]?.let { "@$it" } ?: rulesetName

      else -> rulesetName
    }

  private fun Boolean.toStarlarkString(): String = if (this) "True" else "False"

  private fun List<String>.toStarlarkString(): String = joinToString(prefix = "[", postfix = "]", separator = ", ") { "\"$it\"" }

  suspend fun fetchFilesFromOutputGroups(
    cancelChecker: CancelChecker,
    targetsSpec: TargetsSpec,
    aspect: String,
    outputGroups: List<String>,
    shouldSyncManualFlags: Boolean,
    isRustEnabled: Boolean,
    shouldLogInvocation: Boolean,
  ): BazelBspAspectsManagerResult {
    if (targetsSpec.values.isEmpty()) return BazelBspAspectsManagerResult(BepOutput(), BazelStatus.SUCCESS)
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
    val syncFlags = workspaceContextProvider.currentWorkspaceContext().syncFlags.values

    val flagsToUse = defaultFlags + allowManualTargetsSyncFlags + syncFlags

    return bazelBspCompilationManager
      .buildTargetsWithBep(
        cancelChecker = cancelChecker,
        targetsSpec = targetsSpec,
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
        shouldLogInvocation = shouldLogInvocation,
      ).let { BazelBspAspectsManagerResult(it.bepOutput, it.processResult.bazelStatus) }
  }
}
