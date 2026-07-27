package org.jetbrains.bazel.data

import com.intellij.ide.starter.ide.IDETestContext
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.test.framework.serializeBazelRcPath
import org.jetbrains.bazel.test.framework.toBazelRcPath
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.HexFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.writeText

object BazelProjectConfigurer {
  private const val USER_BAZELRC_IMPORT = "try-import %workspace%/user.bazelrc"
  private val nestedBazelInvocationSequence = AtomicLong()
  private val nestedBazelProcessIdentity = "${ProcessHandle.current().pid()}-${System.currentTimeMillis()}"
  private val windowsNestedBazelRoots = ConcurrentHashMap.newKeySet<Path>()

  init {
    Runtime.getRuntime().addShutdownHook(
      Thread(::cleanupWindowsNestedBazelRoots, "Windows nested Bazel roots cleanup"),
    )
  }

  fun configureProjectBeforeUse(
    context: IDETestContext,
    createProjectView: Boolean = true,
    bazelServerMaxIdleSecs: Int? = null,
  ) {
    runBazelClean(context)
    configureProjectFiles(
      context,
      createProjectView = createProjectView,
      bazelServerMaxIdleSecs = bazelServerMaxIdleSecs,
    )
  }

  @OptIn(ExperimentalPathApi::class)
  fun configureProjectBeforeUseWithoutBazelClean(
    context: IDETestContext,
    removeDotIdea: Boolean = true,
    createProjectView: Boolean = true,
    bazelServerMaxIdleSecs: Int? = null,
  ) {
    configureProjectFiles(context, removeDotIdea, createProjectView, bazelServerMaxIdleSecs)
  }

  @OptIn(ExperimentalPathApi::class)
  private fun configureProjectFiles(
    context: IDETestContext,
    removeDotIdea: Boolean = true,
    createProjectView: Boolean = true,
    bazelServerMaxIdleSecs: Int? = null,
  ) {
    if (removeDotIdea) {
      (context.resolvedBazelProjectHome / ".idea").deleteRecursively()
    }
    (context.resolvedBazelProjectHome / Constants.DOT_BAZELBSP_DIR_NAME).deleteRecursively()
    (context.resolvedBazelProjectHome / "build.gradle").deleteIfExists()
    (context.resolvedBazelProjectHome / "build.gradle.kts").deleteIfExists()
    (context.resolvedBazelProjectHome / "settings.gradle").deleteIfExists()
    (context.resolvedBazelProjectHome / "settings.gradle.kts").deleteIfExists()
    (context.resolvedBazelProjectHome / "gradlew").deleteIfExists()
    (context.resolvedBazelProjectHome / "gradlew.bat").deleteIfExists()
    configureBazelSettings(context, bazelServerMaxIdleSecs)
    if (createProjectView) {
      createProjectViewFile(context)
    }
  }

  fun addHermeticCcToolchain(context: IDETestContext) {
    if (IdeStarterOs.current() == IdeStarterOs.WINDOWS) return
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
    resetFileFromGit(context, "MODULE.bazel")
    if (moduleFile.exists()) {
      moduleFile.toFile().appendText("\n$toolchainConfig")
    } else {
      moduleFile.writeText(toolchainConfig)
    }
  }

  private fun resetFileFromGit(context: IDETestContext, fileName: String) {
    try {
      ProcessBuilder("git", "checkout", "--", fileName)
        .directory(context.resolvedBazelProjectHome.toFile())
        .start()
        .waitFor()
    } catch (_: Exception) {
      // Not a git repo (e.g., synthetic projects extracted from zip) — file is already clean
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

  private fun configureBazelSettings(context: IDETestContext, bazelServerMaxIdleSecs: Int?) {
    val lines = mutableListOf<String>()

    lines.addAll(nestedBazelStartupOptions(context.resolvedBazelProjectHome, registerCleanup = true))
    bazelServerMaxIdleSecs?.let { lines.add("startup --max_idle_secs=$it") }

    val repoCache = System.getenv("IDE_STARTER_BAZEL_REPOSITORY_CACHE")
      ?.let { Path.of(it) }
      ?: System.getProperty("ide.starter.bazel.repository.cache")
        ?.let { Path.of(it) }
      ?: defaultCacheRoot.resolve("repository-cache")
    lines.add(bazelCacheSetting("repository_cache", repoCache))

    val isPerformanceTest = System.getProperty("idea.performance.tests") == "true"
    val diskCache = System.getenv("IDE_STARTER_BAZEL_DISK_CACHE")
      ?.let { Path.of(it) }
      ?: System.getProperty("ide.starter.bazel.disk.cache")
        ?.let { Path.of(it) }
      ?: if (!isPerformanceTest) defaultCacheRoot.resolve("disk-cache") else null
    diskCache?.let { lines.add(bazelCacheSetting("disk_cache", it)) }

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

    writeGeneratedBazelSettings(context.resolvedBazelProjectHome, lines)
  }

  internal fun nestedBazelStartupOptions(
    projectRoot: Path,
    environment: Map<String, String> = System.getenv(),
    hostOs: IdeStarterOs = IdeStarterOs.current(),
    userHome: String = System.getProperty("user.home"),
    invocationId: String? = null,
    registerCleanup: Boolean = false,
  ): List<String> = if (hostOs == IdeStarterOs.WINDOWS && environment.containsKey("TEST_TMPDIR")) {
    val home = userHome.trimEnd('/', '\\')
    val rootKey = windowsNestedBazelRootKey(
      projectRoot,
      environment,
      invocationId ?: "$nestedBazelProcessIdentity-${nestedBazelInvocationSequence.incrementAndGet()}",
    )
    val invocationRoot = "$home/ijr/$rootKey"
    if (registerCleanup) {
      windowsNestedBazelRoots.add(Path.of(userHome).resolve("ijr").resolve(rootKey))
    }
    listOf(
      "startup --batch",
      "startup --output_user_root=${serializeBazelRcPath("$invocationRoot/u")}",
      "startup --install_base=${serializeBazelRcPath("$invocationRoot/i")}",
      "startup --output_base=${serializeBazelRcPath("$invocationRoot/b")}",
    )
  } else {
    emptyList()
  }

  private fun windowsNestedBazelRootKey(
    projectRoot: Path,
    environment: Map<String, String>,
    invocationId: String,
  ): String {
    val normalizedProjectRoot = projectRoot.toAbsolutePath().normalize().toString()
      .replace('\\', '/')
      .lowercase(Locale.US)
    val matrixVariant = environment[USE_BAZEL_VERSION_ENV].orEmpty()
    val digest = MessageDigest.getInstance("SHA-256")
      .digest("$normalizedProjectRoot\n$matrixVariant\n$invocationId".toByteArray(StandardCharsets.UTF_8))
    return HexFormat.of().formatHex(digest, 0, 8)
  }

  internal fun writeGeneratedBazelSettings(projectRoot: Path, lines: List<String>) {
    (projectRoot / "user.bazelrc").writeText(lines.joinToString("\n", postfix = "\n"))
    ensureUserBazelrcImport(projectRoot / ".bazelrc")
  }

  // The import must stay after the project's own settings so the generated options win on conflicts.
  private fun ensureUserBazelrcImport(bazelrc: Path) {
    val existing = if (bazelrc.exists()) bazelrc.toFile().readText() else ""
    val lines = existing.lines()
    val importIndex = lines.indexOfLast { it.trim() == USER_BAZELRC_IMPORT }
    val settingsAfterImport = importIndex >= 0 && lines.drop(importIndex + 1).any {
      it.isNotBlank() && !it.trim().startsWith("#")
    }
    if (importIndex >= 0 && !settingsAfterImport) return
    val withoutImport = lines.filter { it.trim() != USER_BAZELRC_IMPORT }.joinToString("\n").trimEnd('\n')
    bazelrc.writeText(
      buildString {
        append(withoutImport)
        if (isNotEmpty()) appendLine()
        appendLine(USER_BAZELRC_IMPORT)
      },
    )
  }

  internal fun cleanupWindowsNestedBazelRoots() {
    windowsNestedBazelRoots.forEach { root ->
      try {
        if (root.exists()) {
          deleteRecursivelyWithoutFollowingJunctions(root)
        }
        windowsNestedBazelRoots.remove(root)
      } catch (_: Exception) {
        // Best-effort cleanup; a terminated Bazel process may still hold a Windows file lock.
      }
    }
  }

  // Bazel plants junctions inside the output base (execroot source forest, local external repos).
  // NIO reports Windows junctions as directories rather than symlinks, so a plain Files.walk would
  // descend through them and delete the junction targets' contents (e.g. the fixture project).
  private fun deleteRecursivelyWithoutFollowingJunctions(root: Path) {
    Files.walkFileTree(
      root,
      object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
          if (attrs.isSymbolicLink || attrs.isOther || !attrs.isDirectory) {
            Files.deleteIfExists(dir)
            return FileVisitResult.SKIP_SUBTREE
          }
          return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
          Files.deleteIfExists(file)
          return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
          exc?.let { throw it }
          Files.deleteIfExists(dir)
          return FileVisitResult.CONTINUE
        }
      },
    )
  }

  internal fun bazelCacheSetting(name: String, path: Path): String =
    "common --$name=${path.toBazelRcPath()}"

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
    get() = resolvedProjectHome.takeIf { it.isDirectory() && it.name != ".ijwb" } ?: resolvedProjectHome.parent
}
