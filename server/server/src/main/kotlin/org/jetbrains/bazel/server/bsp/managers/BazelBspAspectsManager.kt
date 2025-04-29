package org.jetbrains.bazel.server.bsp.managers

import org.jetbrains.bazel.bazelrunner.params.BazelFlag.aspect
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.buildManualTests
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.buildTagFilters
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.color
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.curses
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.keepGoing
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.outputGroups
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.bep.BepOutput
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bazel.server.bzlmod.BzlmodRepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMapping
import org.jetbrains.bazel.server.bzlmod.RepoMappingDisabled
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
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
  private val bazelRelease: BazelRelease,
) {
  private val aspectsPath = Paths.get(aspectsResolver.bazelBspRoot, Constants.ASPECTS_ROOT)
  private val templateWriter = TemplateWriter(aspectsPath)

  fun calculateRulesetLanguages(
    externalRulesetNames: List<String>,
    externalAutoloads: List<String>,
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
  ): List<RulesetLanguage> =
    Language
      .entries
      .mapNotNull { language ->
        val rulesetName = language.rulesetNames.firstOrNull { it in externalRulesetNames }
        rulesetName?.let {
          return@mapNotNull RulesetLanguage(it, language)
        }
        if (language.isBundled(externalAutoloads)) {
          return@mapNotNull RulesetLanguage(null, language)
        }
        null
      }.removeDisabledLanguages(featureFlags)
      .addNativeAndroidLanguageIfNeeded(workspaceContext, featureFlags)
      .addExternalPythonLanguageIfNeeded(externalRulesetNames, featureFlags)

  private fun Language.isBundled(externalAutoloads: List<String>): Boolean {
    if (!isBundled) return false
    // Bundled in Bazel version < 8
    if (bazelRelease.major < 8) return true
    // If a language is autoloaded in Bazel version >= 8, it effectively restores the old "bundled" behavior.
    return (rulesetNames + autoloadHints).any { it in externalAutoloads }
  }

  private fun List<RulesetLanguage>.removeDisabledLanguages(featureFlags: FeatureFlags): List<RulesetLanguage> {
    val disabledLanguages =
      buildSet {
        if (!featureFlags.isAndroidSupportEnabled) add(Language.Android)
        if (!featureFlags.isGoSupportEnabled) add(Language.Go)
        if (!featureFlags.isCppSupportEnabled) add(Language.Cpp)
        if (!featureFlags.isPythonSupportEnabled) add(Language.Python)
      }
    return filterNot { it.language in disabledLanguages }
  }

  private fun List<RulesetLanguage>.addNativeAndroidLanguageIfNeeded(
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
  ): List<RulesetLanguage> {
    if (!featureFlags.isAndroidSupportEnabled) return this
    if (!workspaceContext.enableNativeAndroidRules.value) return this
    return this.filterNot { it.language == Language.Android } + RulesetLanguage(null, Language.Android)
  }

  private fun List<RulesetLanguage>.addExternalPythonLanguageIfNeeded(
    externalRulesetNames: List<String>,
    featureFlags: FeatureFlags,
  ): List<RulesetLanguage> {
    if (!featureFlags.isPythonSupportEnabled) return this
    val pythonRulesetName = Language.Python.rulesetNames.firstOrNull { externalRulesetNames.contains(it) } ?: return this
    return this.filterNot { it.language == Language.Python } + RulesetLanguage(pythonRulesetName, Language.Python)
  }

  fun generateAspectsFromTemplates(
    rulesetLanguages: List<RulesetLanguage>,
    externalRulesetNames: List<String>,
    workspaceContext: WorkspaceContext,
    toolchains: Map<RulesetLanguage, Label?>,
    bazelRelease: BazelRelease,
    repoMapping: RepoMapping,
    featureFlags: FeatureFlags,
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
          "rulesetName" to ruleLanguage?.calculateCanonicalName(repoMapping).orEmpty(),
          "rulesetNameApparent" to ruleLanguage?.rulesetName.orEmpty(),
          "addTransitiveCompileTimeJars" to
            workspaceContext.experimentalAddTransitiveCompileTimeJars.value.toStarlarkString(),
          "transitiveCompileTimeJarsTargetKinds" to
            workspaceContext.experimentalTransitiveCompileTimeJarsTargetKinds.values.toStarlarkString(),
          "kotlinEnabled" to kotlinEnabled.toString(),
          "javaEnabled" to javaEnabled.toString(),
          "pythonEnabled" to pythonEnabled.toString(),
          // https://github.com/JetBrains/intellij-community/tree/master/build/jvm-rules
          "usesRulesJvm" to ("rules_jvm" in externalRulesetNames).toString(),
          "bazel8OrAbove" to bazel8OrAbove.toString(),
          "toolchainType" to ruleLanguage?.let { rl -> toolchains[rl]?.toString()?.let { "\"" + it + "\"" } },
        )
      templateWriter.writeToFile(templateFilePath, outputFile, variableMap)
    }

    templateWriter.writeToFile(
      Constants.CORE_BZL + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve(Constants.CORE_BZL),
      mapOf(
        "isPropagateExportsFromDepsEnabled" to
          featureFlags.isPropagateExportsFromDepsEnabled.toStarlarkString(),
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
    targetsSpec: TargetsSpec,
    aspect: String,
    outputGroups: List<String>,
    shouldLogInvocation: Boolean,
    workspaceContext: WorkspaceContext,
    originId: String?,
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
    val allowManualTargetsSyncFlags = if (workspaceContext.allowManualTargetsSync.value) listOf(buildManualTests()) else emptyList()
    val syncFlags = workspaceContext.syncFlags.values

    val flagsToUse = defaultFlags + allowManualTargetsSyncFlags + syncFlags

    return bazelBspCompilationManager
      .buildTargetsWithBep(
        targetsSpec = targetsSpec,
        extraFlags = flagsToUse,
        originId = originId,
        environment = emptyList(),
        shouldLogInvocation = shouldLogInvocation,
        workspaceContext = workspaceContext,
      ).let { BazelBspAspectsManagerResult(it.bepOutput, it.processResult.bazelStatus) }
  }
}
