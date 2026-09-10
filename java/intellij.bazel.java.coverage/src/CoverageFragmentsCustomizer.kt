package org.jetbrains.bazel.java.coverage

import com.intellij.execution.configurations.coverage.CoverageFragment
import com.intellij.execution.ui.SettingsEditorFragment
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationEditorCustomizer
import org.jetbrains.bazel.run.config.TargetsFragment
import org.jetbrains.bazel.run.state.CoverageWithBazelFragment
import org.jetbrains.bazel.run.state.RunWithBazelFragment

/**
 * Switches between native IntelliJ and Bazel coverage UI.
 * It depends on the [isJavaAgentCoverageApplicableTo] result.
 */
internal class CoverageFragmentsCustomizer : BazelRunConfigurationEditorCustomizer {

  override fun fragmentsCreated(
    configuration: BazelRunConfiguration,
    fragments: List<SettingsEditorFragment<BazelRunConfiguration, *>>,
  ) {
    val runWithBazel = fragments.findFragment<RunWithBazelFragment>() ?: return
    val bazelCoverage = fragments.findFragment<CoverageWithBazelFragment>() ?: return
    val targets = fragments.findFragment<TargetsFragment>() ?: return
    val useJavaAgentCoverage = { isJavaAgentCoverageApplicableTo(targets.targets, runWithBazel.checkBox.isSelected) }
    bazelCoverage.availableWhen { !useJavaAgentCoverage() }
    runWithBazel.checkBox.addItemListener { selectCoverageUi(fragments, useJavaAgentCoverage()) }
    targets.addTargetsListener { selectCoverageUi(fragments, useJavaAgentCoverage()) }
  }

  override fun fragmentsReset(
    configuration: BazelRunConfiguration,
    fragments: List<SettingsEditorFragment<BazelRunConfiguration, *>>,
  ) {
    selectCoverageUi(fragments, isJavaAgentCoverageApplicableTo(configuration))
  }

  private fun selectCoverageUi(fragments: List<SettingsEditorFragment<BazelRunConfiguration, *>>, useJavaCoverage: Boolean) {
    val useBazelCoverage = !useJavaCoverage
    val javaGroup = fragments.findFragment<CoverageFragment<BazelRunConfiguration>>() ?: return
    val bazelGroup = fragments.findFragment<CoverageWithBazelFragment>() ?: return
    if (javaGroup.component() == null || bazelGroup.component() == null) return
    val javaChildren = javaGroup.children
    val bazelChildren = bazelGroup.children
    if (bazelChildren.all { it.isSelected == useBazelCoverage } && javaChildren.all { it.isSelected == useJavaCoverage }) return
    bazelChildren.forEach { it.isSelected = useBazelCoverage }
    javaChildren.forEach { it.isSelected = useJavaCoverage }
  }
}

private inline fun <reified T> List<SettingsEditorFragment<BazelRunConfiguration, *>>.findFragment(): T? = filterIsInstance<T>()
  .firstOrNull()
