package org.jetbrains.bazel.sync.workspace.languages.scala

import org.jetbrains.bazel.util.BazelRuleSetProvider

internal class ScalaBazelRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): Set<String> = setOf("rules_scala")
}
