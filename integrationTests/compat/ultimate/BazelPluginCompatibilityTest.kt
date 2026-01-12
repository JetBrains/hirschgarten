// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.build

import com.intellij.ide.starter.community.model.BuildType
import com.intellij.ide.starter.extended.ide.CacheRedirectorPublicIdeDownloader
import com.intellij.ide.starter.ide.InstalledIde
import com.intellij.ide.starter.ide.installer.StandardInstaller
import com.intellij.ide.starter.models.IdeProductImp
import jetbrains.buildServer.messages.serviceMessages.PublishArtifacts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.intellij.build.BuildPaths.Companion.COMMUNITY_ROOT
import org.jetbrains.intellij.build.BuildPaths.Companion.ULTIMATE_HOME
import org.jetbrains.intellij.build.PluginVerifier
import org.jetbrains.intellij.build.VerifierIdeInfo
import org.jetbrains.intellij.build.VerifierPluginInfo
import org.jetbrains.intellij.build.buildPlugin
import org.jetbrains.intellij.build.createPluginVerifier
import org.jetbrains.intellij.build.dependencies.BuildDependenciesUtil.cleanDirectory
import org.jetbrains.intellij.build.dependencies.JdkDownloader
import org.jetbrains.intellij.build.dependencies.TeamCityHelper
import org.jetbrains.intellij.build.telemetry.block
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

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
    private const val BAZEL_PLUGIN_ID = "org.jetbrains.bazel"
    private const val BAZEL_PLUGIN_MODULE = "intellij.bazel.plugin"
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
        buildBazelPlugin()
      }

      val installedIde = block("Downloading IDEA Ultimate $ideBuildNumber") {
        downloadIde(ideBuildNumber)
      }

      val verifier = block("Downloading plugin verifier") {
        createPluginVerifier(errorHandler = { loggedErrors.add(it.trim()) })
      }

      val hasErrors = runCompatibilityTest(bazelPlugin, installedIde, verifier)

      if (hasErrors) {
        assertNoNewErrors(loggedErrors, ideBuildNumber)
      }
    }
  }

  private fun assertNoNewErrors(loggedErrors: List<String>, ideBuildNumber: String) {
    val knownErrorsContent = KNOWN_ERRORS_FILE.readText()
    val newErrors = loggedErrors.filter { error -> !knownErrorsContent.contains(error) }

    check(newErrors.isEmpty()) {
      """
        The Bazel plugin built from sources is incompatible with IDEA Ultimate $ideBuildNumber.
        This indicates that API modifications have broken compatibility with the target IDE version.

        New errors (not in known_errors.txt):
        ${newErrors.joinToString("\n")}

        All logged errors:
        ${loggedErrors.joinToString("\n")}

        To accept these errors as known, add them to:
        plugins/bazel/integrationTests/compat/testData/known_errors.txt
      """.trimIndent()
    }

    if (loggedErrors.isNotEmpty()) {
      println("Compatibility check passed with ${loggedErrors.size} known error(s)")
    }
  }

  private suspend fun buildBazelPlugin(): BazelPluginInfo {
    val outRootDir = ULTIMATE_HOME.resolve("out/bazel-plugin")
    cleanDirectory(outRootDir)

    val versions = parseBzlVersions()

    buildPlugin(BAZEL_PLUGIN_MODULE) {
      sinceBuild { versions.sinceVersion }
      untilBuild { versions.untilVersion }
      version { versions.pluginVersion }
      options {
        outRootDir(outRootDir)
      }
    }

    val pluginFile = outRootDir.toFile().walkTopDown()
      .filter { it.isFile && it.name.startsWith("bazel-plugin-") && it.name.endsWith(".zip") && !it.name.endsWith("blockmap.zip") }
      .firstOrNull()
      ?: error("Cannot find built Bazel plugin zip (bazel-plugin-*.zip) in $outRootDir")

    return BazelPluginInfo(
      path = pluginFile.toPath(),
      pluginVersion = versions.pluginVersion,
    )
  }

  private fun parseBzlVersions(): BazelVersions {
    // Try platform-specific versions file first, fall back to default versions.bzl
    val platform = System.getProperty("bazel.plugin.platform")
    val platformVersionsFile = platform?.let { ULTIMATE_HOME.resolve("plugins/bazel/versions.$it.bzl") }
    val defaultVersionsFile = ULTIMATE_HOME.resolve("plugins/bazel/versions.bzl")

    val versionsFile = when {
      platformVersionsFile?.exists() == true -> platformVersionsFile
      defaultVersionsFile.exists() -> defaultVersionsFile
      else -> error("No version file found: tried $platformVersionsFile and $defaultVersionsFile")
    }

    val content = versionsFile.readText()

    fun extractValue(name: String): String {
      val regex = """$name\s*=\s*"([^"]+)"""".toRegex()
      return regex.find(content)?.groupValues?.get(1)
        ?: error("Could not find $name in $versionsFile")
    }

    return BazelVersions(
      pluginVersion = extractValue("INTELLIJ_BAZEL_VERSION"),
      sinceVersion = extractValue("SINCE_VERSION"),
      untilVersion = extractValue("UNTIL_VERSION"),
    )
  }

  private suspend fun downloadIde(buildNumber: String): InstalledIde {
    val ideInfo = IdeProductImp.IU.copy(buildNumber = buildNumber, buildType = BuildType.RELEASE.type)
    return StandardInstaller(
      downloader = CacheRedirectorPublicIdeDownloader,
      customInstallersDownloadDirectory = TeamCityHelper.persistentCachePath,
    ).install(ideInfo).second
  }

  private suspend fun runCompatibilityTest(
    bazelPlugin: BazelPluginInfo,
    installedIde: InstalledIde,
    verifier: PluginVerifier,
  ): Boolean {
    val ideIdentity = "${installedIde.productCode}-${installedIde.build}"

    val ideSpecificHome = ULTIMATE_HOME.resolve("out/bazel-plugin-home/$ideIdentity").also {
      cleanDirectory(it)
    }

    val ideSpecificReportDir = ULTIMATE_HOME.resolve("out/bazel-plugin-reports/$ideIdentity").also {
      cleanDirectory(it)
    }

    return block("Running plugin verifier for $ideIdentity") {
      verifier.verify(
        homeDir = ideSpecificHome,
        reportDir = ideSpecificReportDir,
        plugin = VerifierPluginInfo(
          path = bazelPlugin.path,
          pluginId = BAZEL_PLUGIN_ID,
          buildNumber = bazelPlugin.pluginVersion,
        ),
        ide = VerifierIdeInfo(
          installationPath = installedIde.installationPath,
          productCode = installedIde.productCode,
          productBuild = installedIde.build,
        ),
        runtimeDir = JdkDownloader.getRuntimeHome(COMMUNITY_ROOT),
      ).also {
        println(PublishArtifacts("$ideSpecificReportDir/**=>bazel-plugin-compatibility-report/$ideIdentity"))
      }
    }
  }

  private data class BazelPluginInfo(
    val path: Path,
    val pluginVersion: String,
  )

  private data class BazelVersions(
    val pluginVersion: String,
    val sinceVersion: String,
    val untilVersion: String,
  )
}
