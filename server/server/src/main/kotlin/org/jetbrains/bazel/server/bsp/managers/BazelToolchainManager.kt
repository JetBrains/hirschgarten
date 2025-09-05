package org.jetbrains.bazel.server.bsp.managers

class BazelToolchainManager {
  fun getToolchain(rulesetLanguage: RulesetLanguage): String? =
    when (rulesetLanguage.language) {
      Language.Scala -> """"@${rulesetLanguage.rulesetName}//scala:toolchain_type""""
      Language.Java -> """config_common.toolchain_type("@bazel_tools//tools/jdk:runtime_toolchain_type", mandatory = False)"""
      Language.Kotlin -> """"@${rulesetLanguage.rulesetName}//kotlin/internal:kt_toolchain_type""""
      Language.Go -> """"@${rulesetLanguage.rulesetName}//go:toolchain""""
      else -> null
    }
}
