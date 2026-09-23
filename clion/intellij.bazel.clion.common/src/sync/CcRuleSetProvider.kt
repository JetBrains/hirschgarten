package org.jetbrains.bazel.clion.sync

import com.intellij.aspect.lib.Rules
import org.jetbrains.bazel.clion.BazelCLionFeatureFlags
import org.jetbrains.bazel.util.BazelRuleSet
import org.jetbrains.bazel.util.BazelRuleSetProvider

private val CcRuleSet = BazelRuleSet(
  aspectLanguage = Rules.CC,
  rulesetNames = listOf("rules_cc"),
  isBundled = true,
  autoloadHints = listOf("CcInfo"),
  hostLocations = listOf("https://github.com/bazelbuild/rules_cc"),
)

internal class CcRuleSetProvider : BazelRuleSetProvider {
  override fun ruleSets(): List<BazelRuleSet> =
    if (BazelCLionFeatureFlags.isCLionEnabled) listOf(CcRuleSet) else emptyList()
}
