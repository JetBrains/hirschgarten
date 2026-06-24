package org.jetbrains.bazel.server.bsp.managers

import com.intellij.aspect.lib.AspectConfig
import com.intellij.aspect.lib.Rules
import com.intellij.aspect.lib.deployAspectZip
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.ShowRepoResult
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.aspect
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.buildManualTests
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.keepGoing
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.noRunValidations
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.outputGroups
import org.jetbrains.bazel.bazelrunner.params.BazelFlag.remoteDownloadOutputsTopLevel
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.BzlmodRepoMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.server.bep.BepOutput
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

@ApiStatus.Internal
data class BazelBspAspectsManagerResult(val bepOutput: BepOutput, val status: BazelStatus) {
  val isFailure: Boolean
    get() = status != BazelStatus.SUCCESS

  fun renameNamedSets(runNumber: Int) = BazelBspAspectsManagerResult(bepOutput.renameNamedSets(runNumber), status)

  fun merge(anotherResult: BazelBspAspectsManagerResult): BazelBspAspectsManagerResult =
    BazelBspAspectsManagerResult(bepOutput.merge(anotherResult.bepOutput), status.merge(anotherResult.status))

  companion object {
    fun emptyResult(): BazelBspAspectsManagerResult = BazelBspAspectsManagerResult(BepOutput(), BazelStatus.SUCCESS)
  }
}

@ApiStatus.Internal
sealed interface RuleSetName

@ApiStatus.Internal
data class ApparentRulesetName(val name: String) : RuleSetName
internal data class CanonicalRulesetName(val name: String) : RuleSetName

@ApiStatus.Internal
data class RulesetLanguage(val rulesetName: RuleSetName?, val language: Language)

@ApiStatus.Internal
class BazelBspAspectsManager(
  private val workspaceRoot: Path,
  private val executeService: ExecuteService,
  private val bazelRelease: BazelRelease,
) {
  fun calculateRulesetLanguages(
    externalRulesetNames: List<String>,
    externalRulesetDefinitions: Map<String, ShowRepoResult?>,
    externalAutoloads: List<String>,
  ): List<RulesetLanguage> {
    val httpArchiveUpstreamURLsByCanonicalName =
      externalRulesetDefinitions.values.mapNotNull { definition -> (definition as? ShowRepoResult.HttpArchiveRepository)?.let { it.name to it.urls } }
    val canonicalRepoByHostLocation =
      httpArchiveUpstreamURLsByCanonicalName.flatMap { (k, v) -> v.map { Pair(k, it) } }.associateBy { it.second }
        .mapValues { (k, v) -> v.first }
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
        if (language.isBundledFor(bazelRelease, externalAutoloads)) {
          return@mapNotNull RulesetLanguage(null, language)
        }
        null
      }.removeDisabledLanguages()
      .addExternalPythonLanguageIfNeeded(externalRulesetNames)
  }

  private fun List<RulesetLanguage>.removeDisabledLanguages(): List<RulesetLanguage> {
    val disabledLanguages =
      buildSet {
        if (!BazelFeatureFlags.isGoSupportEnabled) add(Language.Go)
        if (!BazelFeatureFlags.isPythonSupportEnabled) add(Language.Python)
      }
    return filterNot { it.language in disabledLanguages }
  }

  private fun List<RulesetLanguage>.addExternalPythonLanguageIfNeeded(
    externalRulesetNames: List<String>,
  ): List<RulesetLanguage> {
    if (!BazelFeatureFlags.isPythonSupportEnabled) return this
    val pythonRulesetName = Language.Python.rulesetNames.firstOrNull { externalRulesetNames.contains(it) } ?: return this
    return this.filterNot { it.language == Language.Python } + RulesetLanguage(ApparentRulesetName(pythonRulesetName), Language.Python)
  }

  fun deployIntelliJAspect(
    rulesetLanguages: List<RulesetLanguage>,
    bazelRelease: BazelRelease,
    repoMapping: RepoMapping,
  ) {
    val ruleNameMapping = rulesetLanguages.mapNotNull {
      // As the versions of rules_java for bazel 7 do not provide full information, we
      // prefer to the builtin rules.
      if ((it.language == Language.Java) && (bazelRelease.major <= 7)) return@mapNotNull null
      val canonicalRuleName = it.calculateCanonicalName(repoMapping) ?: return@mapNotNull null
      it.language.aspectLanguage to "@${canonicalRuleName}"
    }.toMap()

    // Languages for which the built-in rule set (bazel 8 and earlier) is used.
    // As the versions of rules_java for bazel 7 do not provide full information, we
    // prefer to the builtin rules.
    val builtInLanguages = rulesetLanguages.filter {
      it.calculateCanonicalName(repoMapping) == null
    }.map { it.language.aspectLanguage }.toSet() + (if (bazelRelease.major <= 7) setOf(Rules.JAVA) else setOf())

    deployAspectZip(
      workspaceRoot,
      Path.of(Constants.DOT_BAZELBSP_DIR_NAME),
      AspectConfig(
        "${bazelRelease.major}",
        ruleNameMapping,
        builtInLanguages,
      ),
    )

  }

  private fun RulesetLanguage.calculateCanonicalName(repoMapping: RepoMapping): String? =
    when {
      // If the name is already a conical one, we can take it; however we have the canonical name without
      // prefix, so we have to add @ to indicate it as canonical, as the template adds a single @ as prefix.
      (rulesetName as? CanonicalRulesetName) != null -> "@${rulesetName.name}"
      // bazel mod dump_repo_mapping returns everything without @@
      // and in aspects we have a @ prefix
      repoMapping is BzlmodRepoMapping && (rulesetName as? ApparentRulesetName) != null ->
        repoMapping.apparentRepoNameToCanonicalName[rulesetName.name]?.let { "@$it" } ?: rulesetName.name

      else -> (rulesetName as? ApparentRulesetName)?.name
    }

  suspend fun fetchFilesFromOutputGroups(
    targetsSpec: TargetCollection,
    aspects: List<String>,
    outputGroups: List<String>,
    workspaceContext: WorkspaceContext,
    taskId: TaskId,
  ): BazelBspAspectsManagerResult {
    if (targetsSpec.values.isEmpty()) return BazelBspAspectsManagerResult(BepOutput(), BazelStatus.SUCCESS)
    val defaultFlags =
      listOf(
        aspect(aspects.map { resolveAspectLabel(it) }.joinToString(",")),
        outputGroups(outputGroups),
        keepGoing(),
        // Validations don't contribute to the project model and only slow down sync, so disable them.
        noRunValidations(),
      )
    val allowManualTargetsSyncFlags = if (workspaceContext.allowManualTargetsSync) listOf(buildManualTests()) else emptyList()
    val syncFlags = workspaceContext.syncFlags

    val flagsToUse = defaultFlags + allowManualTargetsSyncFlags + syncFlags

    val emptyBuild = executeService.buildTargetsWithBep(
      targetsSpec = TargetCollection(listOf()),
      extraFlags = flagsToUse + listOf("--ui_event_filters=-WARNING"), // We know, we do an empty build, no need to warn; see https://github.com/bazelbuild/bazel/issues/6811
      taskId = taskId,
    )

    val options = emptyBuild.bepOutput.options
    val existingBuildTagFilters = options.lastOrNull { it.startsWith("--build_tag_filters=") }?.removePrefix("--build_tag_filters=")
    // Ensure remote_download_outputs is at least toplevel so that all required output files are downloaded
    val remoteDownloadOverride =
      if (options.lastOrNull { it.startsWith("--remote_download_outputs=") } == "--remote_download_outputs=minimal") {
        listOf(remoteDownloadOutputsTopLevel())
      }
      else {
        // Do not modify the value if remote_download_outputs is set to a higher value or the default (toplevel) is used
        emptyList()
      }

    return executeService
      .buildTargetsWithBep(
        targetsSpec = targetsSpec,
        extraFlags = flagsToUse + remoteDownloadOverride +
                     listOf("--build_tag_filters=" + existingBuildTagFilters?.let { "$it," }.orEmpty() + "-${Constants.NO_IDE}"),
        taskId = taskId,
      ).let {
        val bepOutput = it.bepOutput
        if (bepOutput.buildToolVersion == BazelRelease.FALLBACK_VERSION) {
          bepOutput.buildToolVersion = bazelRelease
        }
        BazelBspAspectsManagerResult(bepOutput, it.processResult.bazelStatus)
      }
  }

  private fun resolveAspectLabel(aspect: String): String = "//${Constants.DOT_BAZELBSP_DIR_NAME}/$aspect"
}
