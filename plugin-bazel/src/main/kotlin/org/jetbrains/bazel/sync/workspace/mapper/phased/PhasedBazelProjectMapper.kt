package org.jetbrains.bazel.sync.workspace.mapper.phased

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.Language
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.phased.interestingDeps
import org.jetbrains.bazel.commons.phased.isBinary
import org.jetbrains.bazel.commons.phased.isManual
import org.jetbrains.bazel.commons.phased.isNoIde
import org.jetbrains.bazel.commons.phased.isTest
import org.jetbrains.bazel.commons.phased.kind
import org.jetbrains.bazel.commons.phased.name
import org.jetbrains.bazel.commons.phased.resources
import org.jetbrains.bazel.commons.phased.srcs
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BuildTargetCollection
import org.jetbrains.bazel.sync.workspace.languages.jvm.JVMLanguagePluginParser
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

class PhasedBazelProjectMapper(private val bazelPathsResolver: BazelPathsResolver, private val workspaceContext: WorkspaceContext) {
  fun resolveWorkspace(context: PhasedBazelProjectMapperContext, project: PhasedBazelMappedProject): BazelResolvedWorkspace {
    val shouldSyncManualTargets = workspaceContext.allowManualTargetsSync
    val targets =
      project.targets
        .asSequence()
        .map { it.value.target }
        .filter { it.isSupported() }
        .filter { shouldSyncManualTargets || !it.isManual }
        .filterNot { it.isNoIde }
        .map { it.toBspBuildTarget(context, project) }
        .toList()
    return BazelResolvedWorkspace(
      targets = BuildTargetCollection.ofBuildTargets(targets),
      libraries = emptyList(),
      hasError = project.hasError,
    )
  }

  private fun Build.Target.toBspBuildTarget(context: PhasedBazelProjectMapperContext, project: PhasedBazelMappedProject): RawBuildTarget {
    val label = Label.parse(name).assumeResolved()
    return RawBuildTarget(
      id = label,
      tags = inferTags(),
      dependencies = interestingDeps.map { Label.parse(it) },
      kind = inferKind(),
      sources = calculateSources(project),
      resources = calculateResources(project),
      noBuild = isManual, // TODO lol, it's different from the aspect sync??
      baseDirectory = bazelPathsResolver.toDirectoryPath(label, context.repoMapping),
      data = null,
    )
  }

  private fun Build.Target.inferKind(): TargetKind {
    val ruleType =
      when {
        isBinary -> RuleType.BINARY
        isTest -> RuleType.TEST
        else -> RuleType.LIBRARY
      }

    val languageClasses = inferLanguages().mapNotNull { LanguageClass.Companion.fromLanguage(it) }.toSet()

    return TargetKind(
      kindString = kind,
      languageClasses = languageClasses,
      ruleType = ruleType,
    )
  }

  private fun Build.Target.inferTags(): List<String> {
    val manualTag = if (isManual) BuildTargetTag.MANUAL else null

    return listOfNotNull(manualTag)
  }

  private fun Build.Target.inferLanguages(): Set<Language> {
    val languagesForTarget = Language.allOfKind(kind)
    val languagesForSources = srcs.flatMap { Language.allOfSource(it) }.toHashSet()
    return languagesForTarget + languagesForSources
  }

  private fun Build.Target.isSupported(): Boolean {
    val isRuleSupported = Language.allOfKind(kind).isNotEmpty()
    val areSourcesSupported =
      srcs
        .map { it.substringAfterLast('.') }
        .any { Language.allOfSource(".$it").isNotEmpty() }

    return isRuleSupported || areSourcesSupported
  }

  private fun Build.Target.calculateSources(project: PhasedBazelMappedProject): List<SourceItem> {
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

  private fun Build.Target.calculateResources(project: PhasedBazelMappedProject): List<Path> {
    val directResources = resources.calculateFiles()
    val resourcesFromDependencies = resources.calculateModuleDependencies(project).flatMap { it.calculateResources(project) }
    return (directResources + resourcesFromDependencies).distinct()
  }

  private fun List<String>.calculateModuleDependencies(project: PhasedBazelMappedProject): List<Build.Target> =
    mapNotNull { Label.parseOrNull(it) }
      .mapNotNull { project.targets[it]?.target }

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
