package org.jetbrains.bazel.sync.workspace.mapper

import com.google.devtools.build.lib.query2.proto.proto2api.Build
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.commons.phased.generatorName
import org.jetbrains.bazel.commons.phased.interestingDeps
import org.jetbrains.bazel.commons.phased.isManual
import org.jetbrains.bazel.commons.phased.isNoIde
import org.jetbrains.bazel.commons.phased.kind
import org.jetbrains.bazel.commons.phased.name
import org.jetbrains.bazel.commons.phased.resources
import org.jetbrains.bazel.commons.phased.srcs
import org.jetbrains.bazel.commons.phased.tags
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.allowManualTargetsSync
import org.jetbrains.bazel.sync.workspace.snapshot.OutputLocationCollectionBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

@ApiStatus.Internal
class PhasedBazelProjectMapper(
  private val bazelPathsResolver: BazelPathsResolver,
  private val projectView: ProjectView,
) {
  fun mapTargets(
    targets: Map<Label, Build.Target>
  ): List<BuildTarget> {
    val shouldSyncManualTargets = projectView.allowManualTargetsSync
    val targets: List<BuildTarget> =
      targets
        .asSequence()
        .map { it.value }
        .filter { it.isSupported() }
        .filter { shouldSyncManualTargets || !it.isManual }
        .filterNot { it.isNoIde }
        .map { it.toBspBuildTarget(targets) }
        .toList()
    return targets
  }

  private fun Build.Target.toBspBuildTarget(targets: Map<Label, Build.Target>): BuildTarget {
    val label = Label.parse(name).assumeResolved()
    return WorkspaceTarget(
      key = WorkspaceTargetKey(label = label),
      dependencies = interestingDeps.map { DependencyLabel.parse(it) },
      kind = inferKind(),
      sources = OutputLocationCollectionBuilder.ofLocations(calculateSources(targets)),
      resources = OutputLocationCollectionBuilder.ofLocations(calculateResources(targets)),
      data = emptyList(),
      generatorName = generatorName,
      isTestOnly = false,
      tags = tags,
    )
  }

  private fun Build.Target.inferKind(): TargetKind {
    val inferredRuleKind = TargetKindService.getInstance().guessFromRuleName(kind)
    if (inferredRuleKind.languageClasses.isNotEmpty()) return inferredRuleKind
    return inferredRuleKind.copy(languageClasses = languagesFromSources().toSet())
  }

  private fun Build.Target.languagesFromSources(): Sequence<LanguageClass> = srcs.asSequence().mapNotNull {
    LanguageClass.fromExtension(it.substringAfterLast('.'))
  }

  private fun Build.Target.isSupported(): Boolean {
    return TargetKindService.getInstance().findPredefinedRule(kind) != null ||
           languagesFromSources().any()
  }

  private fun Build.Target.calculateSources(targets: Map<Label, Build.Target>): List<OutputLocation> {
    val sourceFiles = srcs.calculateFiles()
    val itemsFromDependencies = srcs.calculateModuleDependencies(targets).flatMap { it.calculateSources(targets) }
    return (sourceFiles + itemsFromDependencies).distinct()
  }

  private fun Build.Target.calculateResources(targets: Map<Label, Build.Target>): List<OutputLocation> {
    val directResources = resources.calculateFiles()
    val resourcesFromDependencies = resources.calculateModuleDependencies(targets).flatMap { it.calculateResources(targets) }
    return (directResources + resourcesFromDependencies).distinct()
  }

  private fun List<String>.calculateModuleDependencies(targets: Map<Label, Build.Target>): List<Build.Target> =
    mapNotNull { Label.parseOrNull(it) }
      .mapNotNull { targets[it] }

  private fun List<String>.calculateFiles(): List<OutputLocation> =
    map { it.bazelFileFormatToWorkspacePath() }
      .filter { bazelPathsResolver.workspaceRoot().resolve(it).let { path -> path.isRegularFile() } }
      .map { OutputLocation.Workspace(it) }

  // `//a/b:c.java` -> `a/b/c.java`
  private fun String.bazelFileFormatToWorkspacePath(): String = replace(':', '/').trimStart('/')
}
