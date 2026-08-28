package org.jetbrains.bazel.test.framework

import com.intellij.openapi.application.PathManager
import org.jetbrains.kotlin.incremental.createDirectory
import java.io.File
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
internal object BazelTestCaches {
  private const val BAZEL_SETTINGS_START = "# BEGIN IntelliJ Bazel unit-test settings"
  private const val BAZEL_SETTINGS_END = "# END IntelliJ Bazel unit-test settings"

  // create %user_home%/bazel-test-temp
  fun setupBazelRc(projectRoot: Path, jvmToolchains: Boolean = true) {
    val bazelCachesPath: String = run {
      val testCaches = File(System.getProperty("user.home"), "bazel-test-temp")
      testCaches.createDirectory()
      serializeBazelRcPath(testCaches.absolutePath)
    }

    val lines = buildList {
      add("startup --host_jvm_args=-Djava.io.tmpdir=$bazelCachesPath")
      add("common --action_env BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=0")
      add("common --action_env BAZEL_NO_APPLE_CPP_TOOLCHAIN=0")
      if (jvmToolchains) {
        add("build --java_runtime_version=remotejdk_21")
      }
      add("build --action_env=TMP=$bazelCachesPath")
      add("build --action_env=TEMP=$bazelCachesPath")
    }
    projectRoot.resolve(".bazelrc").writeText(lines.joinToString("\n"))
  }

  fun configureBazelCaches(projectRoot: Path, testProjectPath: String) {
    val cacheRoot = testCacheRoot()
      .resolve(cacheGroup(testProjectPath))
      .createDirectories()

    val bazeliskCache = cacheRoot.resolve("bazelisk").createDirectories()
    projectRoot.resolve(".bazeliskrc").writeText("BAZELISK_HOME=${bazeliskCache.toBazelPath()}\n")

    val repositoryCache = cacheRoot.resolve("repository-cache").createDirectories()
    val diskCache = cacheRoot.resolve("disk-cache").createDirectories()
    val outputUserRoot = cacheRoot.resolve("output-user-root").createDirectories()
    val outputBase = cacheRoot.resolve("output-bases").resolve(cacheKey(testProjectPath)).createDirectories()
    val lines = listOf(
      "startup --max_idle_secs=${bazelServerMaxIdleSeconds()}",
      "startup --output_user_root=${outputUserRoot.toBazelRcPath()}",
      "startup --output_base=${outputBase.toBazelRcPath()}",
      "common --repository_cache=${repositoryCache.toBazelRcPath()}",
      "common --disk_cache=${diskCache.toBazelRcPath()}",
    )
    writeManagedBazelrcBlock(projectRoot.resolve(".bazelrc"), lines)
  }

  fun findKotlinStdlibInClasspath(): Path {
    val stdlibPath = PathManager.getJarPathForClass(Unit::class.java)
                     ?: error("Cannot find the kotlin-stdlib jar in the test classpath")
    return Path.of(stdlibPath)
  }

  private fun bazelServerMaxIdleSeconds(): Int =
    System.getenv("BAZEL_PLUGIN_TEST_BAZEL_MAX_IDLE_SECONDS")
      ?.toIntOrNull()
    ?: System.getProperty("bazel.plugin.test.bazel.max.idle.seconds")
      ?.toIntOrNull()
    ?: 7200

  private fun testCacheRoot(): Path {
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
