package org.jetbrains.bsp.bazel.server.bsp.managers

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner

class BazelToolchainManager(private val bazelRunner: BazelRunner) {
  fun getToolchain(rulesetLanguage: RulesetLanguage, cancelChecker: CancelChecker): Label? =
    when (rulesetLanguage.language) {
      Language.Scala -> Label.parse("@io_bazel_rules_scala//scala:toolchain_type")
      Language.Java -> Label.parse("@bazel_tools//tools/jdk:runtime_toolchain_type")
      Language.Kotlin -> Label.parse("@${rulesetLanguage.rulesetName}//kotlin/internal:kt_toolchain_type")
      Language.Rust -> Label.parse("@${rulesetLanguage.rulesetName}//rust:toolchain_type")
      Language.Android -> getAndroidToolchain(rulesetLanguage, cancelChecker)
      Language.Go -> Label.parse("@${rulesetLanguage.rulesetName}//go:toolchain")
      else -> null
    }

  /**
   * Built-in Android rules use `@bazel_tools//tools/android:sdk_toolchain_type`.
   * However, starlarkified Android rules (`rules_android`, `build_bazel_rules_android`) can use either the built-in toolchain
   * or `@rules_android//toolchains/android_sdk:toolchain_type` depending on the version.
   */
  fun getAndroidToolchain(rulesetLanguage: RulesetLanguage, cancelChecker: CancelChecker): Label? {
    if (!BspFeatureFlags.isAndroidSupportEnabled) return null
    if (rulesetLanguage.rulesetName == null) return NATIVE_ANDROID_TOOLCHAIN
    val androidToolchain = Label.parse("@${rulesetLanguage.rulesetName}//toolchains/android_sdk:toolchain_type")
    val androidToolchainExists =
      bazelRunner.run {
        val command =
          buildBazelCommand {
            query {
              targets.add(androidToolchain)
            }
          }
        runBazelCommand(command, serverPidFuture = null)
          .waitAndGetResult(cancelChecker)
          .isSuccess
      }
    return if (androidToolchainExists) androidToolchain else NATIVE_ANDROID_TOOLCHAIN
  }

  companion object {
    private val NATIVE_ANDROID_TOOLCHAIN = Label.parse("@bazel_tools//tools/android:sdk_toolchain_type")
  }
}
