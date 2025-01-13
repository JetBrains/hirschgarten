package org.jetbrains.bsp.bazel.server.bsp.managers

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.label.Label
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
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Paths

data class BazelBspAspectsManagerResult(val bepOutput: BepOutput, val status: BazelStatus) {
  val isFailure: Boolean
    get() = status != BazelStatus.SUCCESS

  fun merge(anotherResult: BazelBspAspectsManagerResult): BazelBspAspectsManagerResult =
    BazelBspAspectsManagerResult(bepOutput.merge(anotherResult.bepOutput), status.merge(anotherResult.status))
}

data class RuleLanguage(val ruleName: String?, val language: Language)

class BazelBspAspectsManager(
  private val bazelBspCompilationManager: BazelBspCompilationManager,
  private val aspectsResolver: InternalAspectsResolver,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val featureFlags: FeatureFlags,
  private val bazelRelease: BazelRelease,
) {
  private val aspectsPath = Paths.get(aspectsResolver.bazelBspRoot, Constants.ASPECTS_ROOT)
  private val templateWriter = TemplateWriter(aspectsPath)

  fun calculateRuleLanguages(externalRuleNames: List<String>): List<RuleLanguage> =
    Language
      .entries
      .mapNotNull { language ->
        if (language.isBundled && bazelRelease.major < 8) return@mapNotNull RuleLanguage(null, language) // bundled in Bazel version < 8
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
        if (!featureFlags.isCppSupportEnabled) add(Language.Cpp)
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
    toolchains: Map<RuleLanguage, List<Label>>,
    bazelRelease: BazelRelease,
    repoMapping: RepoMapping,
  ) {
    val languageRuleMap = ruleLanguages.associateBy { it.language }
    val activeLanguages = ruleLanguages.map { it.language }.toSet()
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
          "ruleName" to ruleLanguage?.ruleName,
          "addTransitiveCompileTimeJars" to
            workspaceContext.experimentalAddTransitiveCompileTimeJars.value.toStarlarkString(),
          "kotlinEnabled" to kotlinEnabled.toString(),
          "javaEnabled" to javaEnabled.toString(),
          "pythonEnabled" to pythonEnabled.toString(),
          "bazel8OrAbove" to bazel8OrAbove.toString(),
          // This will only return the first toolchain in the list.
          // At the moment, there is only one place this is used, and it wouldn't make sense for there to be more than one anyway though.
          "toolchainType" to ruleLanguage?.let { rl -> toolchains[rl]?.firstOrNull()?.toString().let { "\"" + it + "\"" } },
        )
      templateWriter.writeToFile(templateFilePath, outputFile, variableMap)
    }

    templateWriter.writeToFile(
      Constants.CORE_BZL + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve(Constants.CORE_BZL),
      mapOf("isPropagateExportsFromDepsEnabled" to featureFlags.isPropagateExportsFromDepsEnabled.toStarlarkString()),
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

  private fun Boolean.toStarlarkString(): String = if (this) "True" else "False"

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

    /*
     * Explicitly specifying a list of output groups should prevent bazel from building the default output group, but it will still include the validations output group unless you specifically exclude it.
     * If the validations output group depends on the results of other more costly actions, they can make this take a long time.
     * https://bazel.build/extending/rules#validation_actions
     */
    val flagsToUse = defaultFlags + allowManualTargetsSyncFlags + syncFlags + listOf("--norun_validations")

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
