package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.PathManager
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.kotlin.incremental.createDirectory
import java.io.File
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Prepares the `.bazelrc` file and the Bazel caches for a test project.
 *
 * The setup keeps the Bazel output base and the caches out of the project root. This avoids VFS root
 * access errors during a test sync.
 */
object BazelTestCaches {
  private const val BAZEL_SETTINGS_START = "# BEGIN IntelliJ Bazel unit-test settings"
  private const val BAZEL_SETTINGS_END = "# END IntelliJ Bazel unit-test settings"

  // The tmpdir is the same for every test, because --action_env=TMP is part of the action key.
  private fun testSettings(jvmToolchains: Boolean): List<String> {
    val bazelCachesPath: String = run {
      val testCaches = File(System.getProperty("user.home"), "bazel-test-temp")
      testCaches.createDirectory()
      serializeBazelRcPath(testCaches.absolutePath)
    }

    return buildList {
      add("startup --host_jvm_args=-Djava.io.tmpdir=$bazelCachesPath")
      add("common --action_env BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=0")
      add("common --action_env BAZEL_NO_APPLE_CPP_TOOLCHAIN=0")
      if (jvmToolchains) {
        add("build --java_runtime_version=remotejdk_21")
      }
      add("build --action_env=TMP=$bazelCachesPath")
      add("build --action_env=TEMP=$bazelCachesPath")
    }
  }

  /**
   * Appends the test settings and the cache settings to the `.bazelrc` in [projectRoot], as one managed block.
   *
   * Call it after the copy of the test project. The block keeps the lines of a project `.bazelrc`, and it comes after
   * them, so its settings win.
   */
  fun configureBazelCaches(
    projectRoot: Path,
    testProjectPath: String,
    outputBase: Path,
    registries: List<URI> = defaultRegistries,
    jvmToolchains: Boolean = true,
  ) {
    val cacheRoot = testCacheRoot()
      .resolve(cacheGroup(testProjectPath))
      .createDirectories()

    val bazelVersion = BazelRelease.fromBazelVersionFile(projectRoot)
    val bazeliskCache = cacheRoot.resolve("bazelisk").createDirectories()
    projectRoot.resolve(".bazeliskrc").writeText("BAZELISK_HOME=${bazeliskCache.toBazelPath()}\n")

    val repositoryCache = cacheRoot.resolve("repository-cache").createDirectories()
    val repoContentsCache = cacheRoot.resolve("repo-contents-cache").createDirectories()
    val diskCache = cacheRoot.resolve("disk-cache").createDirectories()
    val outputUserRoot = cacheRoot.resolve("output-user-root").createDirectories()
    val lines = buildList {
      addAll(testSettings(jvmToolchains))
      add("startup --output_user_root=${outputUserRoot.toBazelRcPath()}")
      add("startup --output_base=${outputBase.toBazelRcPath()}")
      add("common --repository_cache=${repositoryCache.toBazelRcPath()}")
      if (bazelVersion.supportsRepoContentsCache()) {
        // Bazel 8 keeps the cache off, and Bazel 9 puts it into the repository cache. A separate directory lets a
        // cache archive keep the downloads and leave out the extracted repositories, because Bazel can extract them again.
        add("common --repo_contents_cache=${repoContentsCache.toBazelRcPath()}")
      }
      add("common --disk_cache=${diskCache.toBazelRcPath()}")
      add("common --${bazelVersion.downloaderConfigFlag()}=${BazelPathManager.downloaderConfig.toBazelRcPath()}")
      registries.forEach { add("common --registry=$it") }
    }
    writeManagedBazelrcBlock(projectRoot.resolve(".bazelrc"), lines)
  }

  /**
   * The registries of a test project: the local test registry, then the pinned [BazelRegistrySnapshot] of the Bazel Central
   * Registry.
   *
   * The local test registry holds light toolchains, for example `cc_false_toolchain` and `false_launcher`.
   */
  val defaultRegistries: List<URI> by lazy {
    listOf(BazelPathManager.testRegistry.toUri(), BazelRegistrySnapshot.registry(testCacheRoot()).toUri())
  }

  fun findKotlinStdlibInClasspath(): Path {
    val stdlibPath = PathManager.getJarPathForClass(Unit::class.java)
                     ?: error("Cannot find the kotlin-stdlib jar in the test classpath")
    return Path.of(stdlibPath)
  }

  fun testCacheRoot(): Path {
    val cacheRoot = System.getenv("BAZEL_PLUGIN_TEST_CACHE_ROOT")
                      ?.let { Path.of(it) }
                    ?: System.getProperty("bazel.plugin.test.cache.root")
                      ?.let { Path.of(it) }
                    ?: System.getProperty("agent.persistent.cache")
                      ?.takeIf { it.isNotBlank() }
                      ?.let { Path.of(it, "bazel-plugin-test-cache") }
                    ?: System.getenv("AGENT_PERSISTENT_CACHE")
                      ?.takeIf { it.isNotBlank() }
                      ?.let { Path.of(it, "bazel-plugin-test-cache") }
                    ?: localDefaultCacheRoot()
    return cacheRoot.toAbsolutePath()
  }

  /** Tells if the Bazel version knows `--repo_contents_cache`. Bazel 8.3 added it. */
  private fun BazelRelease?.supportsRepoContentsCache(): Boolean {
    if (this == null) return true
    return major > 8 || major == 8 && minor >= 3
  }

  /** Returns the name of the downloader config flag. Bazel 7 knows only the experimental name. */
  private fun BazelRelease?.downloaderConfigFlag(): String {
    if (this == null) return "downloader_config"
    return if (major >= 8) "downloader_config" else "experimental_downloader_config"
  }

  private fun cacheGroup(testProjectPath: String): String =
    if (testProjectPath.startsWith("redcodes/")) "redcodes" else cacheKey(testProjectPath)

  private fun cacheKey(testProjectPath: String): String =
    testProjectPath.replace('/', '_').replace('\\', '_')

  private fun localDefaultCacheRoot(): Path {
    val userHome = Path.of(System.getProperty("user.home"))
    val osName = System.getProperty("os.name")
    return when {
      osName.startsWith("Mac", ignoreCase = true) ->
        userHome.resolve("Library").resolve("Caches").resolve("JetBrains").resolve("bazel-plugin-tests")

      osName.startsWith("Windows", ignoreCase = true) -> {
        val localAppData = System.getenv("LOCALAPPDATA")
                             ?.takeIf { it.isNotBlank() }
                             ?.let { Path.of(it) }
                           ?: userHome.resolve("AppData").resolve("Local")
        localAppData.resolve("JetBrains").resolve("bazel-plugin-tests")
      }

      else -> {
        val cacheHome = System.getenv("XDG_CACHE_HOME")
                          ?.takeIf { it.isNotBlank() }
                          ?.let { Path.of(it) }
                        ?: userHome.resolve(".cache")
        cacheHome.resolve("JetBrains").resolve("bazel-plugin-tests")
      }
    }
  }

  private fun writeManagedBazelrcBlock(bazelrc: Path, lines: List<String>) {
    val existingContent = if (bazelrc.exists()) bazelrc.readText() else ""
    val managedBlockPattern =
      Regex("""(?s)\n?\Q$BAZEL_SETTINGS_START\E.*?\Q$BAZEL_SETTINGS_END\E\n?""")
    val baseContent = existingContent.replace(managedBlockPattern, "\n").trimEnd()
    val managedBlock = buildString {
      appendLine(BAZEL_SETTINGS_START)
      lines.forEach(::appendLine)
      appendLine(BAZEL_SETTINGS_END)
    }
    val separator = if (baseContent.isBlank()) "" else "\n\n"
    bazelrc.writeText(baseContent + separator + managedBlock)
  }

  private fun Path.toBazelPath(): String =
    toAbsolutePath().toString().replace('\\', '/')
}
