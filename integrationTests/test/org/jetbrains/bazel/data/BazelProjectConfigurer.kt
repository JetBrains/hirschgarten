package org.jetbrains.bazel.data

import com.intellij.ide.starter.ide.IDETestContext
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText

object BazelProjectConfigurer {
  fun configureProjectBeforeUse(context: IDETestContext, createProjectView: Boolean = true) {
    runBazelClean(context)
    configureProjectBeforeUseWithoutBazelClean(context, createProjectView)
  }

  @OptIn(ExperimentalPathApi::class)
  fun configureProjectBeforeUseWithoutBazelClean(context: IDETestContext, createProjectView: Boolean = true) {
    (context.resolvedProjectHome / ".idea").deleteRecursively()
    (context.resolvedProjectHome / ".bazelbsp").deleteRecursively()
    (context.resolvedProjectHome / "build.gradle").deleteIfExists()
    (context.resolvedProjectHome / "build.gradle.kts").deleteIfExists()
    (context.resolvedProjectHome / "settings.gradle").deleteIfExists()
    (context.resolvedProjectHome / "settings.gradle.kts").deleteIfExists()
    (context.resolvedProjectHome / "gradlew").deleteIfExists()
    (context.resolvedProjectHome / "gradlew.bat").deleteIfExists()
    if (createProjectView) {
      createProjectViewFile(context)
    }
  }

  fun addHermeticCcToolchain(context: IDETestContext) {
    val moduleFile = context.resolvedProjectHome / "MODULE.bazel"
    val workspaceFile = context.resolvedProjectHome / "WORKSPACE"
    val workspaceBzlFile = context.resolvedProjectHome / "WORKSPACE.bazel"

    when {
      moduleFile.exists() -> {
        val toolchainConfig = """

bazel_dep(name = "hermetic_cc_toolchain", version = "4.1.0")

toolchains = use_extension("@hermetic_cc_toolchain//toolchain:ext.bzl", "toolchains")
use_repo(toolchains, "zig_sdk")

register_toolchains(
    "@zig_sdk//toolchain/...",
    "@zig_sdk//libc_aware/toolchain/...",
)
"""
        moduleFile.toFile().appendText(toolchainConfig)
      }
      workspaceFile.exists() || workspaceBzlFile.exists() -> {
        val targetFile = if (workspaceBzlFile.exists()) workspaceBzlFile else workspaceFile
        val toolchainConfig = """

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

HERMETIC_CC_TOOLCHAIN_VERSION = "v4.1.0"

http_archive(
    name = "hermetic_cc_toolchain",
    sha256 = "65b9f964ffc733bbe8559ff5497a887bbd384fee1d7592f355633d655f0dff4a",
    urls = [
        "https://mirror.bazel.build/github.com/uber/hermetic_cc_toolchain/releases/download/{0}/hermetic_cc_toolchain-{0}.tar.gz".format(HERMETIC_CC_TOOLCHAIN_VERSION),
        "https://github.com/uber/hermetic_cc_toolchain/releases/download/{0}/hermetic_cc_toolchain-{0}.tar.gz".format(HERMETIC_CC_TOOLCHAIN_VERSION),
    ],
)

load("@hermetic_cc_toolchain//toolchain:defs.bzl", zig_toolchains = "toolchains")

zig_toolchains()

register_toolchains(
    "@zig_sdk//toolchain/...",
    "@zig_sdk//libc_aware/toolchain/...",
)
"""
        targetFile.toFile().appendText(toolchainConfig)
      }
    }
  }

  private fun runBazelClean(context: IDETestContext) {
    if (System.getenv("TEST_TMPDIR") != null) {
      return
    }

    val exitCode =
      ProcessBuilder("bazel", "clean", "--expunge")
        .directory(context.resolvedProjectHome.toFile())
        .start()
        .waitFor()
    check(exitCode == 0) { "Bazel clean exited with code $exitCode" }
  }


  private fun createProjectViewFile(context: IDETestContext) {
    val projectView = context.resolvedProjectHome / "projectview.bazelproject"
    // Check env vars first (for values with spaces), fall back to system properties
    // argfile composer on TC doesn't handle spaces in VM options well
    val targets = System.getenv("BAZEL_PERF_TARGET_LIST") ?: System.getProperty("bazel.ide.starter.test.target.list")
    val buildFlags = System.getenv("BAZEL_PERF_BUILD_FLAGS") ?: System.getProperty("bazel.ide.starter.test.build.flags")
    if (projectView.exists() && targets == null && buildFlags == null) return
    projectView.writeText(createTargetsSection(targets) + "\n" + createBuildFlagsSection(buildFlags))
  }

  private fun createTargetsSection(targets: String?): String {
    // we previously handled multiple labels on single line, but now we require no spaces between labels
    val targetList = (targets ?: "//...").split(" ").filter { it.isNotBlank() }
    val targetLines = targetList.joinToString("\n") { "  $it" }
    return "targets:\n$targetLines"
  }

  private fun createBuildFlagsSection(buildFlags: String?): String {
    if (buildFlags == null) return ""
    return """
      build_flags:
        $buildFlags
      """.trimIndent()
  }
}
