package org.jetbrains.bazel.sync.workspace.languages.kotlin

import org.jetbrains.bazel.util.BazelRuleSetProvider

internal class KotlinRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): Set<String> = setOf("rules_kotlin")
}
