package org.jetbrains.bazel.workspace.importer

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.sync.ExecutableTargetsComputer
import org.jetbrains.bsp.protocol.RawBuildTarget
import kotlin.collections.get

@ApiStatus.Internal
object TestTargetClassifier {
  fun calculateTargetsToMarkAsTest(
    targets: Set<RawBuildTarget>,
    labelToTargetInfo: Map<Label, RawBuildTarget>,
  ): Set<Label> =
    calculateTargetsToMarkAsTest(
      targets,
      labelToTargetInfo,
      ExecutableTargetsComputer.calculateExecutableTargets(targets, labelToTargetInfo),
    )

  @VisibleForTesting
  fun calculateTargetsToMarkAsTest(
    targets: Set<RawBuildTarget>,
    labelToTargetInfo: Map<Label, RawBuildTarget>,
    executableTargets: Map<ResolvedLabel, List<Label>>,
  ): Set<Label> {
    val (testTargets, nonTestTargets) =
      targets.partition { it.isTestTarget() }
    val libraryTargets = nonTestTargets.filter { it.kind.ruleType == RuleType.LIBRARY }.map { it.id }
    val directTestDependencies = libraryTargets.filter { library ->
      val executables = executableTargets[library]?.mapNotNull { labelToTargetInfo[it] }.orEmpty()
      val executablesWithoutSelfReference = executables.filterNot { it.id == library }
      val directExecutables = executablesWithoutSelfReference.filter { it.dependsOn(library) }
      return@filter directExecutables.isNotEmpty()
                    && executablesWithoutSelfReference.all { it.isTestTarget() }
                    && directExecutables.any { it.sources.isEmpty() && it.id.packagePath == library.packagePath }
    }.toSet()
    return testTargets.map { it.id }.toSet() + directTestDependencies
  }

  private fun RawBuildTarget.isTestTarget(): Boolean = isTestOnly || kind.ruleType == RuleType.TEST

  private fun RawBuildTarget.dependsOn(label: Label): Boolean =
    dependencies.any { it.targetKey.label == label }
}
