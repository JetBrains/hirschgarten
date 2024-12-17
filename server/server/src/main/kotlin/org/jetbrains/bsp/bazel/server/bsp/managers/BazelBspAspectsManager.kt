package org.jetbrains.bsp.bazel.server.bsp.managers

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.aspect
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.buildManualTests
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.buildTagFilters
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.color
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.curses
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.keepGoing
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag.outputGroups
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.commons.BazelStatus
import org.jetbrains.bsp.bazel.commons.Constants
import org.jetbrains.bsp.bazel.logger.BspClientLogger
import org.jetbrains.bsp.bazel.server.bep.BepOutput
import org.jetbrains.bsp.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Paths

/**
 * If a retry with a clean Bazel server does not help, then more retries should have the same result
 */
private const val MAX_RETRIES_ON_OOM = 1

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
    toolchains: Map<RuleLanguage, Label?>,
    bazelRelease: BazelRelease,
  ) {
    val languageRuleMap = ruleLanguages.associateBy { it.language }
    val activeLanguages = ruleLanguages.map { it.language }.toSet()
    val kotlinEnabled = Language.Kotlin in activeLanguages
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
          "toolchainType" to ruleLanguage?.let { rl -> toolchains[rl]?.toString()?.let { "\"" + it + "\"" } },
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

  suspend fun fetchFilesFromOutputGroups(
    cancelChecker: CancelChecker,
    targetsSpec: TargetsSpec,
    aspect: String,
    outputGroups: List<String>,
    shouldSyncManualFlags: Boolean,
    isRustEnabled: Boolean,
    shouldLogInvocation: Boolean,
    bspClientLogger: BspClientLogger,
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

    val flagsToUse = defaultFlags + allowManualTargetsSyncFlags

    var retries = 0

    var result: BazelBspAspectsManagerResult? = null

    // From OG: Bazel server running out of memory on a build shard is generally caused by Bazel garbage collection bugs.
    // Attempt to work around by resuming with a clean Bazel server.
    while (retries <= MAX_RETRIES_ON_OOM && result == null) {
      if (retries > 0) {
        bspClientLogger.message("Retrying building targets on OOM ($retries/$MAX_RETRIES_ON_OOM) ...")
      }
      result =
        bazelBspCompilationManager
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
          ).takeIf { it.processResult.bazelStatus != BazelStatus.OOM_ERROR }
          ?.let { BazelBspAspectsManagerResult(it.bepOutput, it.processResult.bazelStatus) }
      retries++
    }

    return result ?: error("Maximum retries reached. Could not complete build without OOM.")
  }
}
