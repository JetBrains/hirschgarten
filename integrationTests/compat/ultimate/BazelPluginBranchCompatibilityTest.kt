// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.build

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.intellij.build.BuildPaths.Companion.ULTIMATE_HOME
import org.jetbrains.intellij.build.VerifierIdeInfo
import org.jetbrains.intellij.build.createPluginVerifier
import org.jetbrains.intellij.build.dependencies.TeamCityHelper
import org.jetbrains.intellij.build.dev.BuildRequest
import org.jetbrains.intellij.build.dev.buildProductInProcess
import org.jetbrains.intellij.build.impl.SnapshotBuildNumber
import org.jetbrains.intellij.build.telemetry.block
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Path

private const val PLUGIN_ZIP_ENV = "BAZEL_PLUGIN_COMPATIBILITY_PLUGIN_ZIP"
private const val PLUGIN_ZIP_PROPERTY = "bazel.plugin.compatibility.plugin.zip"
private const val TEST_LABEL_ENV = "BAZEL_PLUGIN_COMPATIBILITY_TEST_LABEL"

private val KNOWN_ERRORS_FILE = ULTIMATE_HOME.resolve("plugins/bazel/integrationTests/compat/testData/known_errors_branch.txt")

/**
 * Tests the compatibility of an already-built Bazel plugin archive against IDEA Ultimate dev-built from the current checkout.
 *
 * The plugin is not built here. [PLUGIN_ZIP_ENV] (or the `-D`[PLUGIN_ZIP_PROPERTY] fallback) holds an absolute path to either a
 * `bazel-plugin-<version>.zip` or a directory containing exactly one of them. Without that variable the class is skipped, so the
 * unfiltered `:integrationTests_test` target does not pay for a full IDE dev build.
 *
 * Unlike [BazelPluginCompatibilityTest], which builds the plugin from sources and verifies it against a released IDE, this test
 * inverts the two sides: the IDE is local and the plugin is external. A failure therefore means an already-published plugin build
 * cannot be loaded by the platform the current branch produces.
 *
 * The test is blind to which channel the archive came from - that choice belongs entirely to the caller. CI runs it once per
 * plugin channel, feeding it the latest nightly and the latest stable Bazel plugin artifacts, on the root branch and on the
 * release-line branches alike.
 *
 * [TEST_LABEL_ENV] optionally names the reported test (`vs <label> plugin`). The name is what TeamCity identifies a test by,
 * so runs against different archives register as distinct tests with separate histories instead of merging into one entry.
 * Without it the label falls back to the file name of the provided path, which is unstable across CI runs by design - CI must
 * pass an explicit label.
 *
 * Locally:
 * ```
 * bazel test //plugins/bazel/integrationTests:branch_compatibility_test \
 *   --test_env=BAZEL_PLUGIN_COMPATIBILITY_PLUGIN_ZIP=/abs/path/to/bazel-plugin-<version>.zip
 * ```
 *
 * Directories used by this test:
 * - `out/bazel-plugin-home` - Plugin verifier home directory
 * - `out/bazel-plugin-reports` - Compatibility reports
 */
@EnabledIfEnvironmentVariable(named = PLUGIN_ZIP_ENV, matches = ".+")
class BazelPluginBranchCompatibilityTest {
  companion object {
    private const val PLATFORM_PREFIX = "idea"
    private const val PRODUCT_CODE = "IU"
  }

  @TestFactory
  fun `Bazel plugin branch compatibility test`(): List<DynamicTest> {
    val providedPluginPath = System.getenv(PLUGIN_ZIP_ENV)
      ?: System.getProperty(PLUGIN_ZIP_PROPERTY)
      ?: error("Required env variable $PLUGIN_ZIP_ENV or JVM property -D$PLUGIN_ZIP_PROPERTY not set. " +
               "Expected an absolute path to a bazel-plugin-<version>.zip, or to a directory containing exactly one. " +
               "Example: -D$PLUGIN_ZIP_PROPERTY=/abs/path/to/bazel-plugin-2025.3.1.zip")

    val label = System.getenv(TEST_LABEL_ENV)?.takeIf { it.isNotBlank() }
                ?: Path.of(providedPluginPath).fileName.toString()

    return listOf(dynamicTest("vs $label plugin") { verifyProvidedPlugin(providedPluginPath) })
  }

  private fun verifyProvidedPlugin(providedPluginPath: String) {
    val bazelPlugin = resolveProvidedBazelPluginZip(Path.of(providedPluginPath))

    runBlocking(Dispatchers.Default) {
      val loggedErrors = mutableListOf<String>()

      val idePath = block("Building IDEA Ultimate from sources") {
        val outputRoot = ULTIMATE_HOME.resolve("out").run { if (TeamCityHelper.isUnderTeamCity) resolve("tests") else this }
        buildProductInProcess(BuildRequest(
          platformPrefix = PLATFORM_PREFIX,
          additionalModules = emptyList(),
          projectDir = ULTIMATE_HOME,
          keepHttpClient = false,
          isBootClassPathCorrect = false,
          classesOutputDirectory = outputRoot.resolve("classes"),
        ))
      }

      val verifier = block("Downloading plugin verifier") {
        createPluginVerifier(errorHandler = { loggedErrors.add(it.trim()) })
      }

      val ide = VerifierIdeInfo(
        installationPath = idePath,
        productCode = PRODUCT_CODE,
        productBuild = SnapshotBuildNumber.VALUE,
      )

      val hasErrors = verifyBazelPluginCompatibility(
        verifier = verifier,
        bazelPlugin = bazelPlugin,
        ide = ide,
        reportArtifactSuffix = "-${bazelPlugin.pluginVersion}",
      )

      if (hasErrors) {
        assertNoNewErrors(
          loggedErrors = loggedErrors,
          knownErrorsFile = KNOWN_ERRORS_FILE,
          failureContext = """
            The Bazel plugin ${bazelPlugin.path.fileName} is incompatible with IDEA Ultimate ${ide.productCode}-${ide.productBuild}
            dev-built from the current checkout. The plugin was built elsewhere, so this is API or packaging drift between that
            plugin build and the branch state of the platform it is being loaded into.
          """.trimIndent(),
        )
      }
    }
  }
}
