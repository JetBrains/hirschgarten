package org.jetbrains.bazel.server.bsp.managers

class BazelToolchainManager {
  fun getToolchain(rulesetLanguage: RulesetLanguage): String? =
    when (rulesetLanguage.language) {
      Language.Scala -> rulesetLanguage.rulesetName?.asReponame()?.let { """"@$it//scala:toolchain_type"""" }
      Language.Java -> """config_common.toolchain_type("@bazel_tools//tools/jdk:runtime_toolchain_type", mandatory = False)"""
      Language.Kotlin -> rulesetLanguage.rulesetName?.asReponame()?.let { """"@$it//kotlin/internal:kt_toolchain_type"""" }
      Language.Go -> rulesetLanguage.rulesetName?.asReponame()?.let { """"@$it//go:toolchain"""" }
      Language.Python -> rulesetLanguage.rulesetName?.asReponame()?.let { """config_common.toolchain_type("@$it//python:toolchain_type", mandatory = False)""" }
      else -> null
    }
}
