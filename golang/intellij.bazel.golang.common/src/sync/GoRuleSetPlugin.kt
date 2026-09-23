package org.jetbrains.bazel.golang.sync

import com.intellij.aspect.lib.Rules
import org.jetbrains.bazel.util.BazelRuleSet
import org.jetbrains.bazel.util.BazelRuleSetProvider

private val GoRuleSet = BazelRuleSet(
  aspectLanguage = Rules.GO,
  rulesetNames = listOf("rules_go", "io_bazel_rules_go"),
  isBundled = false,
  autoloadHints = listOf(),
  hostLocations = listOf("https://github.com/bazel-contrib/rules_go/"),
)

internal class GoRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): List<BazelRuleSet> = listOf(GoRuleSet)
}
