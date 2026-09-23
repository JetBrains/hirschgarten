package org.jetbrains.bazel.sync.workspace.languages.kotlin

import com.intellij.aspect.lib.Rules
import org.jetbrains.bazel.util.BazelRuleSet
import org.jetbrains.bazel.util.BazelRuleSetProvider

private val KotlinRuleSet = BazelRuleSet(
  aspectLanguage = Rules.KOTLIN,
  rulesetNames = listOf("rules_kotlin", "io_bazel_rules_kotlin"),
  isBundled = false,
  autoloadHints = listOf(),
  hostLocations = listOf("https://github.com/bazelbuild/rules_kotlin/"),
)

internal class KotlinRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): List<BazelRuleSet> = listOf(KotlinRuleSet)
}
