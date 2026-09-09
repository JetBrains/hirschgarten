package org.jetbrains.bazel.clion.sync

import org.jetbrains.bazel.clion.BazelCLionFeatureFlags
import org.jetbrains.bazel.util.BazelRuleSetProvider

internal class CcRuleSetProvider : BazelRuleSetProvider {

  override fun ruleSets(): Set<String> = if (BazelCLionFeatureFlags.isCLionEnabled) setOf("rules_cc") else emptySet()
}
