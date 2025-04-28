package org.jetbrains.bazel.server.sync.firstPhase

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import org.jetbrains.bazel.commons.Language
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.server.model.FirstPhaseProject
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bazel.server.sync.languages.JVMLanguagePluginParser
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class FirstPhaseTargetToBspMapper(private val bazelPathsResolver: BazelPathsResolver) {
  fun toWorkspaceBuildTargetsResult(project: FirstPhaseProject): WorkspaceBuildTargetsResult {
    val shouldSyncManualTargets = project.workspaceContext.allowManualTargetsSync.value

    val targets =
      project.modules
        .values
        .asSequence()
        .filter { it.isSupported() }
        .filter { shouldSyncManualTargets || !it.isManual }
        .filterNot { it.isNoIde }
        .map { it.toBspBuildTarget(project) }
        .toList()

    return WorkspaceBuildTargetsResult(targets)
  }

  private fun Target.toBspBuildTarget(project: FirstPhaseProject): BuildTarget {
    val label = Label.parse(name).assumeResolved()
    return BuildTarget(
      id = label,
      tags = inferTags(),
      dependencies = interestingDeps.map { Label.parse(it) },
      kind = inferKind(),
      sources = calculateSources(project),
      resources = calculateResources(project),
      noBuild = isManual, // TODO lol, it's different from the aspect sync??
      baseDirectory = bazelPathsResolver.toDirectoryPath(label, project.repoMapping),
    )
  }

  private fun Target.inferKind(): TargetKind {
    val ruleType =
      when {
        isBinary -> RuleType.BINARY
        isTest -> RuleType.TEST
        else -> RuleType.LIBRARY
      }

    val languagesForTarget = Language.allOfKind(kind).mapNotNull { LanguageClass.fromLanguage(it) }
    val languagesForSources = srcs.flatMap { Language.allOfSource(it) }.distinct().mapNotNull { LanguageClass.fromLanguage(it) }

    return TargetKind(
      kindString = kind,
      languageClasses = (languagesForTarget + languagesForSources).toSet(),
      ruleType = ruleType,
    )
  }

  private fun Target.inferTags(): List<String> {
    val manualTag = if (isManual) BuildTargetTag.MANUAL else null

    return listOfNotNull(manualTag)
  }

  private fun Target.inferLanguages(): Set<Language> {
    val languagesForTarget = Language.allOfKind(kind)
    val languagesForSources = srcs.flatMap { Language.allOfSource(it) }.toHashSet()
    return languagesForTarget + languagesForSources
  }

  private fun Target.isSupported(): Boolean {
    val isRuleSupported = Language.allOfKind(kind).isNotEmpty()
    val areSourcesSupported =
      srcs
        .map { it.substringAfterLast('.') }
        .any { Language.allOfSource(".$it").isNotEmpty() }

    return isRuleSupported || areSourcesSupported
  }

  private fun Target.calculateSources(project: FirstPhaseProject): List<SourceItem> {
    val sourceFiles = srcs.calculateFiles()
    val sources =
      sourceFiles.map {
        SourceItem(
          path = it,
          generated = false,
          jvmPackagePrefix = JVMLanguagePluginParser.calculateJVMSourceRootAndAdditionalData(it),
        )
      }
    val itemsFromDependencies = srcs.calculateModuleDependencies(project).flatMap { it.calculateSources(project) }
    return (sources + itemsFromDependencies).distinct()
  }

  private fun Target.calculateResources(project: FirstPhaseProject): List<Path> {
    val directResources = resources.calculateFiles()
    val resourcesFromDependencies = resources.calculateModuleDependencies(project).flatMap { it.calculateResources(project) }
    return (directResources + resourcesFromDependencies).distinct()
  }

  private fun List<String>.calculateModuleDependencies(project: FirstPhaseProject): List<Target> =
    mapNotNull { Label.parseOrNull(it) }
      .mapNotNull { project.modules[it] }

  private fun List<String>.calculateFiles(): List<Path> =
    map { it.bazelFileFormatToPath() }
      .filter { it.exists() }
      .filter { it.isRegularFile() }

  private fun String.bazelFileFormatToPath(): Path {
    val withoutColons = replace(':', '/')
    val withoutTargetPrefix = withoutColons.trimStart('/')
    val relativePath = Path(withoutTargetPrefix)

    return bazelPathsResolver.workspaceRoot().resolve(relativePath)
  }
}
