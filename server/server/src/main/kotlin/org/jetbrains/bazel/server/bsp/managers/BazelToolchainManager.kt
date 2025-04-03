package org.jetbrains.bazel.server.bsp.managers

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags

class BazelToolchainManager(private val bazelRunner: BazelRunner) {
  suspend fun getToolchain(
    rulesetLanguage: RulesetLanguage,
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
  ): Label? =
    when (rulesetLanguage.language) {
      Language.Scala -> Label.parse("@${rulesetLanguage.rulesetName}//scala:toolchain_type")
      Language.Java -> Label.parse("@bazel_tools//tools/jdk:runtime_toolchain_type")
      Language.Kotlin -> Label.parse("@${rulesetLanguage.rulesetName}//kotlin/internal:kt_toolchain_type")
      Language.Android -> getAndroidToolchain(rulesetLanguage, workspaceContext, featureFlags)
      Language.Go -> Label.parse("@${rulesetLanguage.rulesetName}//go:toolchain")
      else -> null
    }

  /**
   * Built-in Android rules use `@bazel_tools//tools/android:sdk_toolchain_type`.
   * However, starlarkified Android rules (`rules_android`, `build_bazel_rules_android`) can use either the built-in toolchain
   * or `@rules_android//toolchains/android_sdk:toolchain_type` depending on the version.
   */
  suspend fun getAndroidToolchain(
    rulesetLanguage: RulesetLanguage,
    workspaceContext: WorkspaceContext,
    featureFlags: FeatureFlags,
  ): Label? {
    if (!featureFlags.isAndroidSupportEnabled) return null
    if (rulesetLanguage.rulesetName == null) return NATIVE_ANDROID_TOOLCHAIN
    val androidToolchain = Label.parse("@${rulesetLanguage.rulesetName}//toolchains/android_sdk:toolchain_type")
    val androidToolchainExists =
      bazelRunner.run {
        val command =
          buildBazelCommand(workspaceContext) {
            query {
              targets.add(androidToolchain)
            }
          }
        runBazelCommand(command, serverPidFuture = null)
          .waitAndGetResult()
          .isSuccess
      }
    return if (androidToolchainExists) androidToolchain else NATIVE_ANDROID_TOOLCHAIN
  }

  companion object {
    private val NATIVE_ANDROID_TOOLCHAIN = Label.parse("@bazel_tools//tools/android:sdk_toolchain_type")
  }
}
