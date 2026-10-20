package com.intellij.bazel.test

import com.intellij.openapi.application.PathManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText

// the OSS pluginTests/~BUILD runs the plugin tests, so its plugin deps must follow the monorepo ≈≈
internal class OssPluginTestsDepsConsistencyTest {

  companion object {
    private const val PLUGIN_PREFIX = "//plugins/bazel/"
    private const val TEST_LIB_SUFFIX = "_test_lib"
    private const val CLION_REASON = "needs CLion (//CIDR) APIs that the open-source SDK does not provide"

    private val TEST_LIB_CALL = Regex("""jvm_library\(\s*name = "pluginTests_test_lib",.*?\n\)""", RegexOption.DOT_MATCHES_ALL)
    private val DEPS_BLOCK = Regex(""" {4}deps = \[.*?\n {4}],""", RegexOption.DOT_MATCHES_ALL)
    private val QUOTED = Regex("\"([^\"]+)\"")

    private val EXCLUSIONS = mapOf(
      "//plugins/bazel/clion/intellij.bazel.clion.common:bazel-clion-common" to CLION_REASON,
    )
  }

  private val pluginDir: Path = PathManager.getHomeDir().resolve("plugins/bazel")
  private val monorepoBuildFile: Path = pluginDir.resolve("pluginTests/BUILD.bazel")
  private val ossBuildFile: Path = pluginDir.resolve("pluginTests/~BUILD")

  @Test
  fun `check oss plugin tests deps`() {
    val expected = expectedDeps()
    val actual = testLibDeps(ossBuildFile).filter { it.startsWith("//") }
    val missing = expected - actual.toSet()
    val extra = actual - expected.toSet()
    assertTrue(missing.isEmpty() && extra.isEmpty(), syncMessage(missing, extra))
  }

  private fun expectedDeps(): List<String> {
    val deps = testLibDeps(monorepoBuildFile)
    val stale = EXCLUSIONS.keys - deps.toSet()
    assertTrue(stale.isEmpty(), "EXCLUSIONS entries are no longer deps of pluginTests_test_lib, remove them: $stale")
    return deps
      .filter { it.startsWith(PLUGIN_PREFIX) && !it.endsWith(TEST_LIB_SUFFIX) && it !in EXCLUSIONS }
      .map { "//" + it.removePrefix(PLUGIN_PREFIX) }
  }

  private fun testLibDeps(buildFile: Path): List<String> {
    val call = TEST_LIB_CALL.find(buildFile.readText())?.value ?: error("pluginTests_test_lib target not found in $buildFile")
    val deps = DEPS_BLOCK.find(call)?.value ?: error("deps not found in pluginTests_test_lib target of $buildFile")
    return QUOTED.findAll(deps).map { it.groupValues[1] }.toList()
  }

  private fun syncMessage(missing: List<String>, extra: List<String>): String = buildString {
    appendLine("plugins/bazel/pluginTests/~BUILD pluginTests_test_lib deps are out of sync with pluginTests/BUILD.bazel")
    appendLine("Missing:")
    missing.forEach { appendLine("  $it") }
    appendLine("Extra:")
    extra.forEach { appendLine("  $it") }
    append("Fix: add the missing labels to pluginTests/~BUILD and remove the extra ones, ")
    append("or add the module to EXCLUSIONS with a reason.")
  }
}
