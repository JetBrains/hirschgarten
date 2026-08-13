package com.intellij.bazel.python.backend.sync

import org.jetbrains.bazel.util.BazelRuleSetProvider

internal class PythonRuleSetPlugin : BazelRuleSetProvider {
    override fun ruleSets(): Set<String> = setOf("rules_python")
}
