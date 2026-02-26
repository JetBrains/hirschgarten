package org.jetbrains.bazel.server.bsp.managers

import org.jetbrains.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.aspect
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.buildManualTests
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.keepGoing
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.outputGroups
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RepoMappingDisabled
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.server.bep.BepOutput
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.TaskId
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString

data class BazelBspAspectsManagerResult(val bepOutput: BepOutput, val status: BazelStatus) {
  val isFailure: Boolean
    get() = status != BazelStatus.SUCCESS

  fun merge(anotherResult: BazelBspAspectsManagerResult): BazelBspAspectsManagerResult =
    BazelBspAspectsManagerResult(bepOutput.merge(anotherResult.bepOutput), status.merge(anotherResult.status))

  companion object {
    fun emptyResult(): BazelBspAspectsManagerResult = BazelBspAspectsManagerResult(BepOutput(), BazelStatus.SUCCESS)
  }
}

sealed interface RuleSetName
data class ApparentRulesetName(val name: String) : RuleSetName
data class CanonicalRulesetName(val name: String) : RuleSetName

fun RuleSetName.asReponame() = when (this) {
  is CanonicalRulesetName -> "@${name}"
  is ApparentRulesetName -> name
}

data class RulesetLanguage(val rulesetName: RuleSetName?, val language: Language)

class BazelBspAspectsManager(
  private val executeService: ExecuteService,
  private val aspectsResolver: InternalAspectsResolver,
  private val bazelRelease: BazelRelease,
) {
  private val aspectsPath = aspectsResolver.aspectsPath
  private val templateWriter = TemplateWriter(aspectsPath)

  fun calculateRulesetLanguages(
    externalRulesetNames: List<String>,
    externalRulesetDefinitions: Map<String, ShowRepoResult?>,
    externalAutoloads: List<String>,
    featureFlags: FeatureFlags,
  ): List<RulesetLanguage> {
    val httpArchiveUpstreamURLsByCanonicalName = externalRulesetDefinitions.values.mapNotNull { definition -> (definition as? ShowRepoResult.HttpArchiveRepository)?.let { it.name to it.urls }}
    val canonicalRepoByHostLocation = httpArchiveUpstreamURLsByCanonicalName.flatMap  { (k, v) -> v.map { Pair(k, it)}}.associateBy { it.second }.mapValues { (k, v) -> v.first  }
    return Language
      .entries
      .mapNotNull { language ->
        language.hostLocations.firstNotNullOfOrNull { location -> canonicalRepoByHostLocation.keys.firstOrNull { it.startsWith(location) } }
          ?.let {
          return@mapNotNull RulesetLanguage(canonicalRepoByHostLocation[it]?.let { CanonicalRulesetName(it) }, language)
        }
        val rulesetName = language.rulesetNames.firstOrNull { it in externalRulesetNames }
        rulesetName?.let {
          return@mapNotNull RulesetLanguage(ApparentRulesetName(it), language)
        }
        if (language.isBundled(externalAutoloads)) {
          return@mapNotNull RulesetLanguage(null, language)
        }
        null
      }.removeDisabledLanguages(featureFlags)
      .addExternalPythonLanguageIfNeeded(externalRulesetNames, featureFlags)
  }

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
        if (!featureFlags.isGoSupportEnabled) add(Language.Go)
        if (!featureFlags.isPythonSupportEnabled) add(Language.Python)
      }
    return filterNot { it.language in disabledLanguages }
  }

  private fun List<RulesetLanguage>.addExternalPythonLanguageIfNeeded(
    externalRulesetNames: List<String>,
    featureFlags: FeatureFlags,
  ): List<RulesetLanguage> {
    if (!featureFlags.isPythonSupportEnabled) return this
    val pythonRulesetName = Language.Python.rulesetNames.firstOrNull { externalRulesetNames.contains(it) } ?: return this
    return this.filterNot { it.language == Language.Python } + RulesetLanguage(ApparentRulesetName(pythonRulesetName), Language.Python)
  }

  fun detectBazelIgnoreAndErrorOut(
    bazelInfo: BazelInfo,
  ) {
    val bazelIgnore = bazelInfo.workspaceRoot.resolve(Constants.BAZEL_IGNORE_FILE_NAME)
    if (!bazelIgnore.exists()) return

    val lines = try {
      Files.lines(bazelIgnore).map { it.trim() }.toList()
    } catch (_: IOException) {
      return
    }
    if (!lines.contains(Constants.DOT_BAZELBSP_DIR_NAME)) return

    throw IllegalStateException("${bazelIgnore} mentions ${Constants.DOT_BAZELBSP_DIR_NAME} which is needed for sync by the bazel plugin.")
  }

  fun generateAspectsFromTemplates(
    rulesetLanguages: List<RulesetLanguage>,
    externalRulesetNames: List<String>,
    workspaceContext: WorkspaceContext,
    bazelRelease: BazelRelease,
    repoMapping: RepoMapping,
    bazelInfo: BazelInfo,
  ) {
    detectBazelIgnoreAndErrorOut(bazelInfo)

    val languageRuleMap = rulesetLanguages.associateBy { it.language }
    val bazel8OrAbove = bazelRelease.major >= 8
    Language.entries.forEach {
      val ruleLanguage = languageRuleMap[it]

      val outputFile = aspectsPath.resolve(it.toAspectRelativePath())
      val templateFilePath = it.toAspectTemplateRelativePath()
      val canonicalRuleName = ruleLanguage?.calculateCanonicalName(repoMapping).orEmpty()
      val variableMap =
        mapOf(
          "rulesetName" to canonicalRuleName,
          // https://github.com/JetBrains/intellij-community/tree/master/build/jvm-rules
          "usesRulesJvm" to ("rules_jvm" in externalRulesetNames).toString(),
          "bazel8OrAbove" to bazel8OrAbove.toString(),
          "codeGeneratorRules" to workspaceContext.pythonCodeGeneratorRuleNames.toStarlarkString(),
          "bspPath" to Constants.DOT_BAZELBSP_DIR_NAME,
      )
      templateWriter.writeToFile(templateFilePath, outputFile, variableMap)
    }

    templateWriter.writeToFile(
      Constants.CORE_BZL + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve(Constants.CORE_BZL),
      mapOf(
        "bspPath" to Constants.DOT_BAZELBSP_DIR_NAME,
      ),
    )

    // https://bazel.build/rules/lib/builtins/Label#repo_name
    // The canonical name of the repository containing the target referred to by this label, without any leading at-signs (@).
    val starlarkRepoMapping =
      when (repoMapping) {
        is BzlmodRepoMapping -> {
          repoMapping.canonicalRepoNameToLocalPath
            .map { (key, value) ->
              "\"${key.dropWhile { it == '@' }}\": \"${value.invariantSeparatorsPathString}\""
            }.joinToString(",\n", "{\n", "\n}")
        }

        is RepoMappingDisabled -> "{}"
      }

    templateWriter.writeToFile(
      "utils/make_variables.bzl" + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve("utils").resolve("make_variables.bzl"),
      mapOf(),
    )

    templateWriter.writeToFile(
      "utils/utils.bzl" + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve("utils").resolve("utils.bzl"),
      mapOf(
        "bspPath" to Constants.DOT_BAZELBSP_DIR_NAME,
        "repoMapping" to starlarkRepoMapping,
      ),
    )

    templateWriter.writeToFile(
      "utils/jvm_common.bzl" + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve("utils").resolve("jvm_common.bzl"),
      mapOf(
        "bspPath" to Constants.DOT_BAZELBSP_DIR_NAME,
      ),
    )

    templateWriter.writeToFile(
      "rules/protobuf/proto_common.bzl" + Constants.TEMPLATE_EXTENSION,
      aspectsPath.resolve("rules").resolve("protobuf").resolve("proto_common.bzl"),
      mapOf(
        "bspPath" to Constants.DOT_BAZELBSP_DIR_NAME,
      ),
    )

  }

  private fun RulesetLanguage.calculateCanonicalName(repoMapping: RepoMapping): String? =
    when {
      // If the name is already a conical one, we can take it; however we have the canonical name without
      // prefix, so we have to add @ to indicate it as canonical, as the template adds a single @ as prefix.
      (rulesetName as? CanonicalRulesetName) != null-> "@${rulesetName.name}"
      // bazel mod dump_repo_mapping returns everything without @@
      // and in aspects we have a @ prefix
      repoMapping is BzlmodRepoMapping && (rulesetName as? ApparentRulesetName)!= null ->
        repoMapping.apparentRepoNameToCanonicalName[rulesetName.name ]?.let { "@$it" } ?: rulesetName.name

      else -> (rulesetName as? ApparentRulesetName)?.name
    }

  private fun List<String>.toStarlarkString(): String = joinToString(prefix = "[", postfix = "]", separator = ", ") { "\"$it\"" }

  suspend fun fetchFilesFromOutputGroups(
    targetsSpec: TargetCollection,
    aspect: String,
    outputGroups: List<String>,
    workspaceContext: WorkspaceContext,
    taskId: TaskId,
  ): BazelBspAspectsManagerResult {
    if (targetsSpec.values.isEmpty()) return BazelBspAspectsManagerResult(BepOutput(), BazelStatus.SUCCESS)
    val defaultFlags =
      listOf(
        aspect(aspectsResolver.resolveLabel(aspect)),
        outputGroups(outputGroups),
        keepGoing(),
        "--remote_download_outputs=toplevel",
      )
    val allowManualTargetsSyncFlags = if (workspaceContext.allowManualTargetsSync) listOf(buildManualTests()) else emptyList()
    val syncFlags = workspaceContext.syncFlags

    val flagsToUse = defaultFlags + allowManualTargetsSyncFlags + syncFlags

    return executeService
      .buildTargetsWithBep(
        targetsSpec = targetsSpec,
        extraFlags = flagsToUse,
        taskId = taskId,
      ).let { BazelBspAspectsManagerResult(it.bepOutput, it.processResult.bazelStatus) }
  }
}
