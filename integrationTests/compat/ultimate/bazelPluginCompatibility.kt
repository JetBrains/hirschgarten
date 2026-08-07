// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.bazel.build

import jetbrains.buildServer.messages.serviceMessages.PublishArtifacts
import org.jetbrains.intellij.build.BuildOptionsSpec
import org.jetbrains.intellij.build.BuildPaths.Companion.COMMUNITY_ROOT
import org.jetbrains.intellij.build.BuildPaths.Companion.ULTIMATE_HOME
import org.jetbrains.intellij.build.PluginVerifier
import org.jetbrains.intellij.build.VerifierIdeInfo
import org.jetbrains.intellij.build.VerifierPluginInfo
import org.jetbrains.intellij.build.buildPlugin
import org.jetbrains.intellij.build.dependencies.BuildDependenciesUtil.cleanDirectory
import org.jetbrains.intellij.build.dependencies.JdkDownloader
import org.jetbrains.intellij.build.telemetry.block
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.readText

internal const val BAZEL_PLUGIN_ID: String = "org.jetbrains.bazel"
internal const val BAZEL_PLUGIN_MODULE: String = "intellij.bazel.plugin"

private const val BAZEL_PLUGIN_ZIP_PREFIX = "bazel-plugin-"
private const val BLOCKMAP_ZIP_SUFFIX = "blockmap.zip"

internal data class BazelPluginInfo(
  val path: Path,
  val pluginVersion: String,
)

internal data class BazelVersions(
  val pluginVersion: String,
  val sinceVersion: String,
  val untilVersion: String,
)

/**
 * Reads `plugins/bazel/versions.<platform>.bzl` when `-Dbazel.plugin.platform` names a platform that has such a file,
 * and `plugins/bazel/versions.bzl` otherwise.
 */
internal fun parseBzlVersions(): BazelVersions {
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

internal suspend fun buildBazelPluginZip(
  outRootDir: Path,
  versions: BazelVersions,
  configureBuildOptions: BuildOptionsSpec.() -> Unit = {},
): BazelPluginInfo {
  cleanDirectory(outRootDir)

  buildPlugin(BAZEL_PLUGIN_MODULE) {
    sinceBuild { versions.sinceVersion }
    untilBuild { versions.untilVersion }
    version { versions.pluginVersion }
    options {
      outRootDir(outRootDir)
      configureBuildOptions()
    }
  }

  val pluginFile = findBazelPluginZips(outRootDir).firstOrNull()
                   ?: error("Cannot find built Bazel plugin zip (bazel-plugin-*.zip) in $outRootDir")

  return BazelPluginInfo(
    path = pluginFile,
    pluginVersion = versions.pluginVersion,
  )
}

private fun isBazelPluginZip(fileName: String): Boolean =
  fileName.startsWith(BAZEL_PLUGIN_ZIP_PREFIX) && fileName.endsWith(".zip") && !fileName.endsWith(BLOCKMAP_ZIP_SUFFIX)

private fun findBazelPluginZips(rootDir: Path): Sequence<Path> =
  rootDir.toFile().walkTopDown().filter { it.isFile && isBazelPluginZip(it.name) }.map { it.toPath() }

/**
 * Resolves a plugin archive built outside this test: [path] is either the archive itself or a directory holding exactly one.
 *
 * The version is taken from the file name because the verifier stores its report under the plugin version it reads from
 * `plugin.xml`, and [verifyBazelPluginCompatibility] has to look the report up by the same value.
 */
internal fun resolveProvidedBazelPluginZip(path: Path): BazelPluginInfo {
  check(path.exists()) { "Bazel plugin path does not exist: $path" }

  val pluginFile = if (path.isDirectory()) {
    val candidates = findBazelPluginZips(path).toList()
    when {
      candidates.isEmpty() -> error("No bazel-plugin-*.zip found in directory $path")
      candidates.size > 1 -> error("Expected exactly one bazel-plugin-*.zip in directory $path, " +
                                   "found ${candidates.size}: ${candidates.joinToString { it.name }}")
      else -> candidates.single()
    }
  }
  else {
    path
  }

  check(isBazelPluginZip(pluginFile.name)) {
    "Expected a plugin archive named bazel-plugin-<version>.zip, got ${pluginFile.name}: $pluginFile"
  }

  return BazelPluginInfo(
    path = pluginFile,
    pluginVersion = pluginFile.name.removePrefix(BAZEL_PLUGIN_ZIP_PREFIX).removeSuffix(".zip"),
  )
}

/**
 * The verifier replaces every character outside this set with `_` when it names its report directories
 * (`DirectoryBasedPluginVerificationReportage.createPluginVerificationDirectory` in intellij-plugin-verifier), while
 * [PluginVerifier.reportVerifierIssues] resolves the directory by the raw [VerifierPluginInfo.buildNumber], so the value
 * has to be pre-sanitized the same way. Nightly plugin versions contain `+` (`2026.2.1-nightly.40+a174936a2fe57`).
 */
private val VERIFIER_REPORT_DIR_INVALID_CHARS = Regex("[^a-zA-Z0-9.#\\-() ]")

private fun String.asVerifierReportDirName(): String = replace(VERIFIER_REPORT_DIR_INVALID_CHARS, "_")

/**
 * [reportArtifactSuffix] keeps the published reports apart when one CI build verifies several plugin archives against the same IDE.
 */
internal suspend fun verifyBazelPluginCompatibility(
  verifier: PluginVerifier,
  bazelPlugin: BazelPluginInfo,
  ide: VerifierIdeInfo,
  reportArtifactSuffix: String = "",
): Boolean {
  val ideIdentity = "${ide.productCode}-${ide.productBuild}"

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
        buildNumber = bazelPlugin.pluginVersion.asVerifierReportDirName(),
      ),
      ide = ide,
      runtimeDir = JdkDownloader.getRuntimeHome(COMMUNITY_ROOT),
    ).also {
      println(PublishArtifacts("$ideSpecificReportDir/**=>bazel-plugin-compatibility-report/$ideIdentity$reportArtifactSuffix"))
    }
  }
}

internal fun assertNoNewErrors(loggedErrors: List<String>, knownErrorsFile: Path, failureContext: String) {
  val knownErrorsContent = knownErrorsFile.readText()
  val newErrors = loggedErrors.filter { error -> !knownErrorsContent.contains(error) }

  check(newErrors.isEmpty()) {
    """
      $failureContext

      New errors (not in ${knownErrorsFile.name}):
      ${newErrors.joinToString("\n")}

      All logged errors:
      ${loggedErrors.joinToString("\n")}

      To accept these errors as known, add them to:
      ${ULTIMATE_HOME.relativize(knownErrorsFile).invariantSeparatorsPathString}
    """.trimIndent()
  }

  if (loggedErrors.isNotEmpty()) {
    println("Compatibility check passed with ${loggedErrors.size} known error(s)")
  }
}
