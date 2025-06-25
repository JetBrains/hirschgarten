package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.util.EnvironmentUtil
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.psi.toBazelVersionLiteral
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

object BazelVersionWorkspaceResolver {
  fun resolveBazelVersionFromWorkspace(workspace: Path): BazelVersionLiteral? {
    return resolveBazelVersionEnvVariable()
      ?: resolveBazeliskRcVersion(workspace)
      ?: resolveWorkspaceBazelVersionFile(workspace)
      ?: resolveFallbackBazelVersion()
  }

  private fun resolveWorkspaceBazelVersionFile(workspace: Path): BazelVersionLiteral? {
    val file = workspace.resolve(".bazelversion")
    if (!file.isRegularFile() || !file.exists()) {
      return null
    }
    return file.readText().toBazelVersionLiteral()
  }

  private fun resolveBazelVersionEnvVariable(): BazelVersionLiteral? =
    EnvironmentUtil.getValue("USE_BAZEL_VERSION")?.toBazelVersionLiteral()

  private fun resolveFallbackBazelVersion(): BazelVersionLiteral? {
    val fallbackVersion = EnvironmentUtil.getValue("USE_BAZEL_FALLBACK_VERSION") ?: return null
    val allowedPrefixes = listOf("error:", "warn:", "silent:")
    val prefix = allowedPrefixes.firstOrNull { fallbackVersion.startsWith(it) }
    return if (prefix == null) {
      fallbackVersion.toBazelVersionLiteral()
    } else {
      fallbackVersion.substring(prefix.length).toBazelVersionLiteral()
    }
  }

  private fun resolveBazeliskRcVersion(workspace: Path): BazelVersionLiteral? {
    val file = workspace
      .resolve(".bazeliskrc") ?: return null
    if (!file.isRegularFile() || !file.exists()) {
      return null
    }
    val properties = BazeliskrcParser.parse(file.readText())
    val version = properties["USE_BAZEL_VERSION"] ?: return null
    return version.toBazelVersionLiteral()
  }
}

// https://github.com/bazelbuild/bazelisk/blob/2ecd43c25b475cab2cd554f0fce40304f2bf3445/config/config.go#L50
object BazeliskrcParser {
  fun parse(content: String): Map<String, String> =
    content.lines()
      .filterNot { it.startsWith("#") }
      .map { it.split("=") }
      .filter { it.size >= 2 }
      .associate { it[0].trim() to it[1].trim() }
}
