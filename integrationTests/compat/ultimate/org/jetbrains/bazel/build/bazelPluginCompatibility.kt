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
import kotlin.io.path.name
import kotlin.io.path.readText

internal const val BAZEL_PLUGIN_ID: String = "org.jetbrains.bazel"
internal const val BAZEL_PLUGIN_MODULE: String = "intellij.bazel.plugin"

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

  val pluginFile = outRootDir.toFile().walkTopDown()
    .filter { it.isFile && it.name.startsWith("bazel-plugin-") && it.name.endsWith(".zip") && !it.name.endsWith("blockmap.zip") }
    .firstOrNull()
    ?: error("Cannot find built Bazel plugin zip (bazel-plugin-*.zip) in $outRootDir")

  return BazelPluginInfo(
    path = pluginFile.toPath(),
    pluginVersion = versions.pluginVersion,
  )
}

internal suspend fun verifyBazelPluginCompatibility(
  verifier: PluginVerifier,
  bazelPlugin: BazelPluginInfo,
  ide: VerifierIdeInfo,
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
        buildNumber = bazelPlugin.pluginVersion,
      ),
      ide = ide,
      runtimeDir = JdkDownloader.getRuntimeHome(COMMUNITY_ROOT),
    ).also {
      println(PublishArtifacts("$ideSpecificReportDir/**=>bazel-plugin-compatibility-report/$ideIdentity"))
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
