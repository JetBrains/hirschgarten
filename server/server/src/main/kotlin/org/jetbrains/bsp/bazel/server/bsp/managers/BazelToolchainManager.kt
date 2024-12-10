package org.jetbrains.bsp.bazel.server.bsp.managers

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.protocol.FeatureFlags

class BazelToolchainManager(private val bazelRunner: BazelRunner, private val featureFlags: FeatureFlags) {
  fun getToolchain(ruleLanguage: RuleLanguage, cancelChecker: CancelChecker): String? =
    when (ruleLanguage.language) {
      Language.Scala -> """"@io_bazel_rules_scala//scala:toolchain_type""""
      Language.Java -> """"@bazel_tools//tools/jdk:runtime_toolchain_type""""
      Language.Kotlin -> """"@${ruleLanguage.ruleName}//kotlin/internal:kt_toolchain_type""""
      Language.Rust -> """"@${ruleLanguage.ruleName}//rust:toolchain_type""""
      Language.Android -> getAndroidToolchain(ruleLanguage, cancelChecker)
      Language.Go -> """"@${ruleLanguage.ruleName}//go:toolchain""""
      else -> null
    }

  /**
   * Built-in Android rules use `@bazel_tools//tools/android:sdk_toolchain_type`.
   * However, starlarkified Android rules (`rules_android`, `build_bazel_rules_android`) can use either the built-in toolchain
   * or `@rules_android//toolchains/android_sdk:toolchain_type` depending on the version.
   */
  fun getAndroidToolchain(ruleLanguage: RuleLanguage, cancelChecker: CancelChecker): String? {
    if (!featureFlags.isAndroidSupportEnabled) return null
    if (ruleLanguage.ruleName == null) return NATIVE_ANDROID_TOOLCHAIN
    val androidToolchain = """"@${ruleLanguage.ruleName}//toolchains/android_sdk:toolchain_type""""
    val androidToolchainExists =
      bazelRunner.run {
        val command =
          buildBazelCommand {
            query {
              targets.add(BuildTargetIdentifier(androidToolchain))
            }
          }
        runBazelCommand(command, serverPidFuture = null)
          .waitAndGetResult(cancelChecker)
          .isSuccess
      }
    return if (androidToolchainExists) androidToolchain else NATIVE_ANDROID_TOOLCHAIN
  }

  companion object {
    private const val NATIVE_ANDROID_TOOLCHAIN = """"@bazel_tools//tools/android:sdk_toolchain_type""""
  }
}
