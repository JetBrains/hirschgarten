package org.jetbrains.bazel.sync.workspace.mapper.phased

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.phased.generatorName
import org.jetbrains.bazel.commons.phased.interestingDeps
import org.jetbrains.bazel.commons.phased.isManual
import org.jetbrains.bazel.commons.phased.isNoIde
import org.jetbrains.bazel.commons.phased.kind
import org.jetbrains.bazel.commons.phased.name
import org.jetbrains.bazel.commons.phased.resources
import org.jetbrains.bazel.commons.phased.srcs
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@ApiStatus.Internal
class PhasedBazelProjectMapper(
  private val bazelPathsResolver: BazelPathsResolver,
  private val workspaceContext: WorkspaceContext
) {
  fun mapTargets(
    repoMapping: RepoMapping,
    targets: Map<Label, Build.Target>
  ): List<RawBuildTarget> {
    val shouldSyncManualTargets = workspaceContext.allowManualTargetsSync
    val targets: List<RawBuildTarget> =
      targets
        .asSequence()
        .map { it.value }
        .filter { it.isSupported() }
        .filter { shouldSyncManualTargets || !it.isManual }
        .filterNot { it.isNoIde }
        .map { it.toBspBuildTarget(repoMapping, targets) }
        .toList()
    return targets
  }

  private fun Build.Target.toBspBuildTarget(repoMapping: RepoMapping, targets: Map<Label, Build.Target>): RawBuildTarget {
    val label = Label.parse(name).assumeResolved()
    return RawBuildTarget(
      id = label,
      dependencies = interestingDeps.map { DependencyLabel.parse(it) },
      kind = inferKind(),
      sources = calculateSources(targets),
      generatedSources = emptyList(),
      resources = calculateResources(targets),
      baseDirectory = bazelPathsResolver.toDirectoryPath(label, repoMapping),
      data = emptyList(),
      generatorName = generatorName,
      isManual = isManual,
      isWorkspace = true, // TODO
    )
  }

  private fun Build.Target.inferKind(): TargetKind {
    val targetKindService = TargetKindService.getInstance()
    val inferredRuleKind = targetKindService.guessFromRuleName(kind)
    if (inferredRuleKind.languageClasses.isNotEmpty()) return inferredRuleKind
    val languagesForSources = languagesFromSources()
    return inferredRuleKind.copy(languageClasses = languagesForSources)
  }

  private fun Build.Target.languagesFromSources(): Set<LanguageClass> = srcs.mapNotNullTo(hashSetOf()) {
    LanguageClass.fromExtension(it.substringAfterLast('.'))
  }

  private fun Build.Target.isSupported(): Boolean {
    val targetKindService = TargetKindService.getInstance()
    val isRuleSupported = targetKindService.fromRuleName(kind) != null
    val areSourcesSupported = languagesFromSources().isNotEmpty()

    return isRuleSupported || areSourcesSupported
  }

  private fun Build.Target.calculateSources(targets: Map<Label, Build.Target>): List<Path> {
    val sourceFiles = srcs.calculateFiles()
    val itemsFromDependencies = srcs.calculateModuleDependencies(targets).flatMap { it.calculateSources(targets) }
    return (sourceFiles + itemsFromDependencies).distinct()
  }

  private fun Build.Target.calculateResources(targets: Map<Label, Build.Target>): List<Path> {
    val directResources = resources.calculateFiles()
    val resourcesFromDependencies = resources.calculateModuleDependencies(targets).flatMap { it.calculateResources(targets) }
    return (directResources + resourcesFromDependencies).distinct()
  }

  private fun List<String>.calculateModuleDependencies(targets: Map<Label, Build.Target>): List<Build.Target> =
    mapNotNull { Label.parseOrNull(it) }
      .mapNotNull { targets[it] }

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
