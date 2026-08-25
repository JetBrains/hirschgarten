package org.jetbrains.bazel.jvm.run

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.languages.starlark.references.findReferredPackage
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasEnv
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmBuildTarget
import org.jetbrains.bazel.target.getTargetDataForLabel
import org.jetbrains.bazel.target.targetStorage
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id

@ApiStatus.Internal
fun BuildTarget.usesJetBrainsTestRunner(project: Project): Boolean =
  JetBrainsTestRunner.TAG in tags ||
  usesJetBrainsTestRunnerCompat(project) ||
  JetBrainsTestRunner.Detector.anyDetects(project, id)

// only needed for some time to keep the JetBrains test runner recognizable for outdated branches (without "jetbrains_test_runner" tag)
private fun BuildTarget.usesJetBrainsTestRunnerCompat(project: Project): Boolean {
  return kind.ruleType == RuleType.TEST &&
         project.targetStorage.getTargetDataForLabel<JvmBuildTarget>(id)?.mainClass in wellKnownJetBrainsTestRunnerImpls
}

private val wellKnownJetBrainsTestRunnerImpls = setOf(
  "com.intellij.tests.JUnit5BazelRunner", // monorepo
  "jetbrains.datalore.buildScripts.JUnit5TestLauncher", // datalore
)

internal fun BazelRunConfiguration.targetsUseJetBrainsTestRunner(): Boolean =
  targetsUseJetBrainsTestRunner(project, targets)

internal fun targetsUseJetBrainsTestRunner(project: Project, targets: List<Label>): Boolean {
  if (targets.isEmpty()) return false
  val expandedTargets = targets.asSequence().flatMap { expandWildcardTarget(project, it) }
  val targetStorage = project.targetStorage
  return expandedTargets.all { label ->
    targetStorage.getTargetSummary(label)?.usesJetBrainsTestRunner(project) ?: JetBrainsTestRunner.Detector.anyDetects(project, label)
  }
}

private fun expandWildcardTarget(project: Project, target: Label): List<Label> {
  if (target.target !is AllRuleTargets) return listOf(target)
  val testableSummaries = project.targetStorage.allTestableSummaries()
  if (testableSummaries.isEmpty()) return listOf(target)  // Monorepo with JPS case
  val baseDirectory = findReferredPackage(project, target.assumeResolved())?.toNioPathOrNull()
                      ?: return listOf(target)
  return testableSummaries
    .filter { it.baseDirectory.startsWith(baseDirectory) }
    .map { it.id }
           .takeIf { it.isNotEmpty() } ?: listOf(target)
}

@ApiStatus.Internal
object JetBrainsTestRunner {

  const val TAG: String = "jetbrains_test_runner"

  internal const val IDE_SM_RUN: String = "JB_IDE_SM_RUN"

  internal const val TEST_FILTER: String = "JB_TEST_FILTER"

  internal const val TEST_UNIQUE_IDS: String = "JB_TEST_UNIQUE_IDS"

  @ApiStatus.Internal
  fun envs(testFilter: String?): Map<String, String> = when (testFilter) {
    null -> mapOf(IDE_SM_RUN to "true")
    else -> mapOf(IDE_SM_RUN to "true", TEST_FILTER to testFilter)
  }

  internal fun setTestUniqueIds(state: BazelRunConfigurationState<*>, testUniqueIds: List<String>) {
    (state as? HasTestFilter)?.testFilter = null
    (state as? HasEnv)?.env?.envs?.let {
      it.remove(TEST_FILTER)
      it[TEST_UNIQUE_IDS] = testUniqueIds.joinToString(separator = ";")
      it[IDE_SM_RUN] = "true"
    }
  }

  internal fun getTestUniqueIds(state: BazelRunConfigurationState<*>): List<String>? {
    (state as? HasEnv)?.env?.envs?.let {
      return it[TEST_UNIQUE_IDS]?.split(";")
    }
    return null
  }

  /**
   * Recognizes targets running on the JetBrains test runner that carry no [JetBrainsTestRunner.TAG], because they are
   * absent from [org.jetbrains.bazel.target.targetStorage].
   *
   * It's only needed for monorepo with JPS - see `MonorepoJetBrainsTestRunnerDetector.kt`.
   * If it's gone this EP can be removed.
   */
  @ApiStatus.Internal
  interface Detector {
    fun usesJetBrainsTestRunner(project: Project, label: Label): Boolean

    companion object {
      val ep: ExtensionPointName<Detector> =
        ExtensionPointName.create("org.jetbrains.bazel.jetBrainsTestRunnerDetector")

      internal fun anyDetects(project: Project, label: Label): Boolean =
        ep.lazySequence().any { it.usesJetBrainsTestRunner(project, label) }
    }
  }
}
