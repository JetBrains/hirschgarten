package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.util.BazelRuleSetProvider

internal class JavaBazelRuleSetPlugin : BazelRuleSetProvider {
  override fun ruleSets(): Set<String> = setOf("rules_java")
}
