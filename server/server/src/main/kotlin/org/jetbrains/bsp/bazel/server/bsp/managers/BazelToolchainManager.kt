package org.jetbrains.bsp.bazel.server.bsp.managers

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.server.bsp.managers.Language
import org.jetbrains.bsp.protocol.FeatureFlags

class BazelToolchainManager(private val bazelRunner: BazelRunner, private val featureFlags: FeatureFlags) {
  fun getToolchain(ruleLanguage: RuleLanguage, cancelChecker: CancelChecker): List<Label> =
    when (ruleLanguage.language) {
      Language.Scala -> listOf(Label.parse("@io_bazel_rules_scala//scala:toolchain_type"), Label.parse("@rules_scala_annex//rules/scala:toolchain_type"))
      Language.Java -> listOf(Label.parse("@bazel_tools//tools/jdk:runtime_toolchain_type"))
      Language.Kotlin -> listOf(Label.parse("@${ruleLanguage.ruleName}//kotlin/internal:kt_toolchain_type"))
      Language.Rust -> listOf(Label.parse("@${ruleLanguage.ruleName}//rust:toolchain_type"))
      Language.Android -> getAndroidToolchain(ruleLanguage, cancelChecker)
      Language.Go -> listOf(Label.parse("@${ruleLanguage.ruleName}//go:toolchain"))
      else -> emptyList()
    }

  /**
   * Built-in Android rules use `@bazel_tools//tools/android:sdk_toolchain_type`.
   * However, starlarkified Android rules (`rules_android`, `build_bazel_rules_android`) can use either the built-in toolchain
   * or `@rules_android//toolchains/android_sdk:toolchain_type` depending on the version.
   */
  fun getAndroidToolchain(ruleLanguage: RuleLanguage, cancelChecker: CancelChecker): List<Label> {
    if (!featureFlags.isAndroidSupportEnabled) return emptyList()
    if (ruleLanguage.ruleName == null) return listOf(NATIVE_ANDROID_TOOLCHAIN)
    val androidToolchain = Label.parse("@${ruleLanguage.ruleName}//toolchains/android_sdk:toolchain_type")
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
    return listOf(if (androidToolchainExists) androidToolchain else NATIVE_ANDROID_TOOLCHAIN).filterNotNull()
  }

  companion object {
    private val NATIVE_ANDROID_TOOLCHAIN = Label.parse("@bazel_tools//tools/android:sdk_toolchain_type")
  }
}
