// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.build

import com.intellij.ide.starter.community.model.BuildType
import com.intellij.ide.starter.extended.ide.CacheRedirectorPublicIdeDownloader
import com.intellij.ide.starter.ide.InstalledIde
import com.intellij.ide.starter.ide.installer.StandardInstaller
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.tools.ide.starter.product.idea.ultimate.IdeaUltimate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.intellij.build.BuildPaths.Companion.ULTIMATE_HOME
import org.jetbrains.intellij.build.VerifierIdeInfo
import org.jetbrains.intellij.build.createPluginVerifier
import org.jetbrains.intellij.build.dependencies.TeamCityHelper
import org.jetbrains.intellij.build.telemetry.block
import org.junit.jupiter.api.Test

private val KNOWN_ERRORS_FILE = ULTIMATE_HOME.resolve("plugins/bazel/integrationTests/compat/testData/known_errors.txt")

/**
 * Tests the compatibility of the Bazel plugin built from sources against a fixed IDEA release version.
 *
 * The IDE build number must be specified via the JVM property:
 * `-Dbazel.plugin.compatibility.ide.build.number=253.22441.33`
 *
 * This allows CI to configure which IDE build to test against (e.g., from `versions.bzl` BENCHMARK_BUILD_NUMBER).
 *
 * Directories used by this test:
 * - `out/bazel-plugin` - Bazel plugin build output
 * - `out/bazel-plugin-home` - Plugin verifier home directory
 * - `out/bazel-plugin-reports` - Compatibility reports
 */
class BazelPluginCompatibilityTest {
  companion object {
    private const val IDE_BUILD_NUMBER_PROPERTY = "bazel.plugin.compatibility.ide.build.number"
  }

  @Test
  fun `Bazel plugin IDE compatibility test`() {
    val ideBuildNumber = System.getenv("BAZEL_PLUGIN_COMPATIBILITY_IDE_BUILD_NUMBER")
      ?: System.getProperty(IDE_BUILD_NUMBER_PROPERTY)
      ?: error("Required env variable BAZEL_PLUGIN_COMPATIBILITY_IDE_BUILD_NUMBER or " +
               "JVM property -D$IDE_BUILD_NUMBER_PROPERTY not set. " +
               "Example: -D$IDE_BUILD_NUMBER_PROPERTY=253.22441.33")

    runBlocking(Dispatchers.Default) {
      val loggedErrors = mutableListOf<String>()

      val bazelPlugin = block("Building Bazel plugin") {
        buildBazelPluginZip(
          outRootDir = ULTIMATE_HOME.resolve("out/bazel-plugin"),
          versions = parseBzlVersions(),
        )
      }

      val installedIde = block("Downloading IDEA Ultimate $ideBuildNumber") {
        downloadIde(ideBuildNumber)
      }

      val verifier = block("Downloading plugin verifier") {
        createPluginVerifier(errorHandler = { loggedErrors.add(it.trim()) })
      }

      val hasErrors = verifyBazelPluginCompatibility(
        verifier = verifier,
        bazelPlugin = bazelPlugin,
        ide = VerifierIdeInfo(
          installationPath = installedIde.installationPath,
          productCode = installedIde.productCode,
          productBuild = installedIde.build,
        ),
      )

      if (hasErrors) {
        assertNoNewErrors(
          loggedErrors = loggedErrors,
          knownErrorsFile = KNOWN_ERRORS_FILE,
          failureContext = """
            The Bazel plugin built from sources is incompatible with IDEA Ultimate $ideBuildNumber.
            This indicates that API modifications have broken compatibility with the target IDE version.
          """.trimIndent(),
        )
      }
    }
  }

  private suspend fun downloadIde(buildNumber: String): InstalledIde {
    val ideInfo = IdeInfo.IdeaUltimate.copy(buildNumber = buildNumber, buildType = BuildType.RELEASE.type)
    return StandardInstaller(
      downloader = CacheRedirectorPublicIdeDownloader,
      customInstallersDownloadDirectory = TeamCityHelper.persistentCachePath,
    ).install(ideInfo).second
  }
}
