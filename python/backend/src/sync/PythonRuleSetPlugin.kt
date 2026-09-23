package com.intellij.bazel.python.backend.sync

import com.intellij.aspect.lib.Rules
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.util.BazelRuleSet
import org.jetbrains.bazel.util.BazelRuleSetProvider

@ApiStatus.Internal
val PythonRuleSet: BazelRuleSet = BazelRuleSet(
  aspectLanguage = Rules.PYTHON,
  rulesetNames = listOf("rules_python"),
  isBundled = true,
  autoloadHints = listOf("PyInfo"),
  hostLocations = listOf("https://github.com/bazel-contrib/rules_python/"),
)

internal class PythonRuleSetPlugin : BazelRuleSetProvider {
    override fun ruleSets(): List<BazelRuleSet> = listOf(PythonRuleSet)
}
