package org.jetbrains.bazel.clion.sync

import org.jetbrains.bazel.util.BazelRuleSetProvider

internal class CLionBazelRuleSets : BazelRuleSetProvider {
  override fun ruleSets(): Set<String>  = setOf("rules_cc")
}
