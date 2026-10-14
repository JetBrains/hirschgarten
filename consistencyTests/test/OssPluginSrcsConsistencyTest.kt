package com.intellij.bazel.test

import com.intellij.openapi.application.PathManager
import com.intellij.util.SystemProperties
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

// the OSS build packs plugins/bazel/~BUILD into the plugin zip, so its srcs must follow the monorepo ij_plugin
internal class OssPluginSrcsConsistencyTest {

  companion object {
    private const val UPDATE_PROPERTY_KEY = "bazel.oss.plugin.srcs.update"
    private const val MAIN_PACKAGE = "intellij.bazel.plugin"
    private const val PLUGIN_PREFIX = "//plugins/bazel/"
    private const val SRCS_COMMENT = "Checked against intellij.bazel.plugin/BUILD.bazel by OssPluginSrcsConsistencyTest."
    private const val CLION_REASON = "needs CLion (//CIDR) APIs that the open-source SDK does not provide"

    private val IJ_PLUGIN_CALL = Regex("""ij_plugin\(\s*name = "plugin_plugin",.*?\n\)""", RegexOption.DOT_MATCHES_ALL)
    private val ZIP_CALL = Regex("""intellij_plugin_zip_and_debug_target\(\s*name = "plugin-bazel",.*?\n\)""", RegexOption.DOT_MATCHES_ALL)
    private val SRCS_BLOCK = Regex("""(    # [^\n]*\n)?    srcs = \[.*?\n    \],""", RegexOption.DOT_MATCHES_ALL)
    private val CONTENT_MODULES = Regex("""content_modules = \[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
    private val DESCRIPTOR_MODULE = Regex("descriptor_module = \"([^\"]+)\"")
    private val MODULE_NAME = Regex("module_name = \"([^\"]+)\"")
    private val QUOTED = Regex("\"([^\"]+)\"")

    private val EXCLUSIONS = mapOf(
      "intellij.bazel.clion.common" to CLION_REASON,
      "intellij.bazel.clion.core" to CLION_REASON,
      "intellij.bazel.sh" to "the open-source SDK has no sh plugin target yet",
    )
  }

  private val pluginDir: Path = PathManager.getHomeDir().resolve("plugins/bazel")
  private val rootBuildFile: Path = pluginDir.resolve("~BUILD")

  @Test
  fun `check oss plugin srcs`() {
    val expected = expectedLabels()
    val actual = actualLabels()
    val missing = expected - actual.toSet()
    val extra = actual - expected.toSet()
    assertTrue(missing.isEmpty() && extra.isEmpty(), syncMessage(missing, extra))
    assertSrcsDirectoriesHaveBuildFiles(actual)
  }

  @Test
  fun `update oss plugin srcs`() {
    if (!SystemProperties.getBooleanProperty(UPDATE_PROPERTY_KEY, false)) {
      assumeFalse(true, "Set -D$UPDATE_PROPERTY_KEY=true to rewrite $rootBuildFile")
    }

    val labels = expectedLabels()
    val text = rootBuildFile.readText()
    val call = ZIP_CALL.find(text)?.value ?: error("plugin-bazel target not found in $rootBuildFile")
    val updatedCall = SRCS_BLOCK.replace(call) { renderSrcs(labels) }
    rootBuildFile.writeText(text.replace(call, updatedCall))
  }

  private fun expectedLabels(): List<String> {
    val call = IJ_PLUGIN_CALL.find(pluginDir.resolve("$MAIN_PACKAGE/BUILD.bazel").readText())?.value
               ?: error("ij_plugin(name = plugin_plugin) not found in $MAIN_PACKAGE/BUILD.bazel")
    val descriptor = DESCRIPTOR_MODULE.find(call)?.groupValues?.get(1) ?: error("descriptor_module not found in ij_plugin")
    val contentBlock = CONTENT_MODULES.find(call)?.groupValues?.get(1) ?: error("content_modules not found in ij_plugin")

    val mainDir = packageOf(descriptor)
    val contentModules = QUOTED.findAll(contentBlock)
      .map { packageOf(it.groupValues[1]) }
      .distinct()
      .associateWith { moduleName(it) }

    val stale = EXCLUSIONS.keys - contentModules.values.toSet()
    assertTrue(stale.isEmpty(), "EXCLUSIONS entries are no longer content modules of ij_plugin, remove them: $stale")

    val content = contentModules
      .filterValues { it !in EXCLUSIONS }
      .map { (dir, module) -> ossLabel(dir, module) }
    return listOf(ossLabel(mainDir, moduleName(mainDir))) + content
  }

  private fun actualLabels(): List<String> {
    val call = ZIP_CALL.find(rootBuildFile.readText())?.value ?: error("plugin-bazel target not found in $rootBuildFile")
    val srcs = SRCS_BLOCK.find(call)?.value ?: error("srcs not found in plugin-bazel target")
    return QUOTED.findAll(srcs).map { it.groupValues[1] }.toList()
  }

  private fun assertSrcsDirectoriesHaveBuildFiles(labels: List<String>) {
    val missing = labels
      .map { it.removePrefix("//").substringBefore(':') }
      .distinct()
      .filterNot { dir ->
        val srcsDir = pluginDir.resolve(dir)
        srcsDir.resolve("~BUILD").exists() || srcsDir.resolve("BUILD.bazel").exists()
      }
    assertTrue(missing.isEmpty(), "OSS srcs directories without ~BUILD or BUILD.bazel under plugins/bazel: $missing")
  }

  private fun packageOf(label: String): String {
    if (label.startsWith(":")) return MAIN_PACKAGE
    require(label.startsWith(PLUGIN_PREFIX)) { "Unexpected ij_plugin label: $label" }
    return label.removePrefix(PLUGIN_PREFIX).substringBefore(':')
  }

  private fun moduleName(dir: String): String {
    val moduleDir = pluginDir.resolve(dir)
    val buildFile = moduleDir.resolve("~BUILD")
    if (buildFile.exists()) {
      MODULE_NAME.find(buildFile.readText())?.let { return it.groupValues[1] }
    }
    val imlFiles = moduleDir.listDirectoryEntries("*.iml")
    check(imlFiles.size == 1) { "$moduleDir has no module_name in ~BUILD and ${imlFiles.size} .iml files" }
    return imlFiles.single().name.removeSuffix(".iml")
  }

  private fun ossLabel(dir: String, module: String): String =
    if (dir.substringAfterLast('/') == module) "//$dir" else "//$dir:$module"

  private fun syncMessage(missing: List<String>, extra: List<String>): String = buildString {
    appendLine("plugins/bazel/~BUILD plugin-bazel srcs are out of sync with ij_plugin in intellij.bazel.plugin/BUILD.bazel")
    appendLine("Missing from srcs:")
    missing.forEach { appendLine("  $it") }
    appendLine("Extra in srcs:")
    extra.forEach { appendLine("  $it") }
    append("Fix: run 'update oss plugin srcs' with -D$UPDATE_PROPERTY_KEY=true, ")
    append("or add the module to EXCLUSIONS with a reason.")
  }

  private fun renderSrcs(labels: List<String>): String = buildString {
    appendLine("    # $SRCS_COMMENT")
    appendLine("    srcs = [")
    labels.forEach { appendLine("        \"$it\",") }
    append("    ],")
  }
}
