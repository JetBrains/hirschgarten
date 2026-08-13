package org.jetbrains.bazel.golang.sync

import org.jetbrains.bazel.util.BazelRuleSetProvider

internal class GoRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): Set<String> = setOf("rules_go")
}
