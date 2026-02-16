package org.jetbrains.bazel.data

import com.intellij.ide.starter.ide.IDETestContext
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

object BazelProjectConfigurer {
  fun configureProjectBeforeUse(context: IDETestContext, createProjectView: Boolean = true) {
    runBazelClean(context)
    configureProjectBeforeUseWithoutBazelClean(context, createProjectView)
  }

  @OptIn(ExperimentalPathApi::class)
  fun configureProjectBeforeUseWithoutBazelClean(context: IDETestContext, createProjectView: Boolean = true) {
    (context.resolvedBazelProjectHome / ".idea").deleteRecursively()
    (context.resolvedBazelProjectHome / ".bazelbsp").deleteRecursively()
    (context.resolvedBazelProjectHome / "build.gradle").deleteIfExists()
    (context.resolvedBazelProjectHome / "build.gradle.kts").deleteIfExists()
    (context.resolvedBazelProjectHome / "settings.gradle").deleteIfExists()
    (context.resolvedBazelProjectHome / "settings.gradle.kts").deleteIfExists()
    (context.resolvedBazelProjectHome / "gradlew").deleteIfExists()
    (context.resolvedBazelProjectHome / "gradlew.bat").deleteIfExists()
    configureBazelSettings(context)
    if (createProjectView) {
      createProjectViewFile(context)
    }
  }

  fun addHermeticCcToolchain(context: IDETestContext) {
    val moduleFile = context.resolvedBazelProjectHome / "MODULE.bazel"
    val toolchainConfig = """
bazel_dep(name = "hermetic_cc_toolchain", version = "4.1.0")

toolchains = use_extension("@hermetic_cc_toolchain//toolchain:ext.bzl", "toolchains")
use_repo(toolchains, "zig_sdk")

register_toolchains(
    "@zig_sdk//toolchain/...",
    "@zig_sdk//libc_aware/toolchain/...",
)
"""
    if (moduleFile.exists()) {
      moduleFile.toFile().appendText("\n$toolchainConfig")
    } else {
      moduleFile.writeText(toolchainConfig)
    }
  }

  private fun runBazelClean(context: IDETestContext) {
    if (System.getenv("TEST_TMPDIR") != null) {
      return
    }

    val exitCode =
      ProcessBuilder("bazel", "clean", "--expunge")
        .directory(context.resolvedBazelProjectHome.toFile())
        .start()
        .waitFor()
    check(exitCode == 0) { "Bazel clean exited with code $exitCode" }
  }


  private val defaultCacheRoot: Path =
    Path.of(System.getProperty("user.home"), ".cache", "ide-starter-bazel")

  private fun configureBazelSettings(context: IDETestContext) {
    val bazelrc = context.resolvedBazelProjectHome / ".bazelrc"
    val lines = mutableListOf<String>()

    val repoCache = System.getenv("IDE_STARTER_BAZEL_REPOSITORY_CACHE")
      ?.let { Path.of(it) }
      ?: System.getProperty("ide.starter.bazel.repository.cache")
        ?.let { Path.of(it) }
      ?: defaultCacheRoot.resolve("repository-cache")
    lines.add("common --repository_cache=$repoCache")

    val isPerformanceTest = System.getProperty("idea.performance.tests") == "true"
    val diskCache = System.getenv("IDE_STARTER_BAZEL_DISK_CACHE")
      ?.let { Path.of(it) }
      ?: System.getProperty("ide.starter.bazel.disk.cache")
        ?.let { Path.of(it) }
      ?: if (!isPerformanceTest) defaultCacheRoot.resolve("disk-cache") else null
    diskCache?.let { lines.add("common --disk_cache=$it") }

    val downloaderConfigSource = System.getenv("IDE_STARTER_BAZEL_DOWNLOADER_CONFIG")
      ?.let { Path.of(it) }
      ?: System.getProperty("ide.starter.bazel.downloader.config")
        ?.let { Path.of(it) }
    if (downloaderConfigSource != null && downloaderConfigSource.exists()) {
      val configFile = context.resolvedBazelProjectHome / "bazel_downloader.cfg"
      val content = downloaderConfigSource.toFile().readText()
        .lineSequence()
        .filter { !it.trim().startsWith("block ") }
        .joinToString("\n")
      configFile.writeText(content)
      val flagName = resolveDownloaderConfigFlag(context)
      lines.add("common --$flagName=bazel_downloader.cfg")
    }

    lines.add("common --java_runtime_version=remotejdk_21")
    lines.add("common --java_language_version=21")
    lines.add("common --tool_java_runtime_version=remotejdk_21")

    bazelrc.toFile().appendText("\n" + lines.joinToString("\n") + "\n")
  }

  private fun resolveDownloaderConfigFlag(context: IDETestContext): String {
    val bazelVersionFile = context.resolvedBazelProjectHome / ".bazelversion"
    if (!bazelVersionFile.exists()) return "experimental_downloader_config"
    val majorVersion = bazelVersionFile.toFile().readText().trim()
      .split(".").firstOrNull()?.toIntOrNull() ?: return "experimental_downloader_config"
    return if (majorVersion >= 8) "downloader_config" else "experimental_downloader_config"
  }

  private fun createProjectViewFile(context: IDETestContext) {
    val projectView = context.resolvedBazelProjectHome / "projectview.bazelproject"
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

  private val IDETestContext.resolvedBazelProjectHome: Path
    get() = resolvedProjectHome.takeIf { it.isDirectory() } ?: resolvedProjectHome.parent
}
