package org.jetbrains.bazel.sync.workspace.languages.scala

import com.intellij.aspect.lib.Rules
import org.jetbrains.bazel.util.BazelRuleSet
import org.jetbrains.bazel.util.BazelRuleSetProvider

private val ScalaRuleSet = BazelRuleSet(
  aspectLanguage = Rules.SCALA,
  rulesetNames = listOf("rules_scala", "io_bazel_rules_scala"),
  isBundled = false,
  autoloadHints = listOf(),
  hostLocations = listOf("https://github.com/bazel-contrib/rules_scala/"),
)

internal class ScalaBazelRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): List<BazelRuleSet> = listOf(ScalaRuleSet)
}
