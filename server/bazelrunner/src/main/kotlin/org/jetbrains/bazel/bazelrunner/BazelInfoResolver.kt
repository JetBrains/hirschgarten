package org.jetbrains.bazel.bazelrunner

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.orLatestSupported
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Paths

private const val RELEASE = "release"
private const val EXECUTION_ROOT = "execution_root"
private const val OUTPUT_BASE = "output_base"
private const val WORKSPACE = "workspace"
private const val BAZEL_BIN = "bazel-bin"
private const val STARLARK_SEMANTICS = "starlark-semantics"

class BazelInfoResolver(private val bazelRunner: BazelRunner) {
  suspend fun resolveBazelInfo(workspaceContext: WorkspaceContext): BazelInfo {
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        info {
          options.addAll(listOf(RELEASE, EXECUTION_ROOT, OUTPUT_BASE, WORKSPACE, BAZEL_BIN, STARLARK_SEMANTICS))
        }
      }
    val processResult =
      bazelRunner
        .runBazelCommand(command, serverPidFuture = null)
        .waitAndGetResult(true)
    return parseBazelInfo(processResult)
  }

  private fun parseBazelInfo(bazelProcessResult: BazelProcessResult): BazelInfo {
    val outputMap =
      bazelProcessResult
        .stdoutLines
        .mapNotNull { line ->
          INFO_LINE_PATTERN.matchEntire(line)?.let { it.groupValues[1] to it.groupValues[2] }
        }.toMap()

    fun extract(name: String): String =
      outputMap[name]
        ?: error(
          "Failed to resolve $name from bazel info in ${bazelRunner.workspaceRoot}. " +
            "Bazel Info output: '${bazelProcessResult.stderrLines.joinToString("\n")}'",
        )

    val bazelReleaseVersion =
      BazelRelease.fromReleaseString(extract(RELEASE))
        ?: bazelRunner.workspaceRoot?.let { BazelRelease.fromBazelVersionFile(it) }.orLatestSupported()

    val starlarkSemantics = parseStarlarkSemantics(extract(STARLARK_SEMANTICS), bazelReleaseVersion)

    return BazelInfo(
      execRoot = Paths.get(extract(EXECUTION_ROOT)),
      outputBase = Paths.get(extract(OUTPUT_BASE)),
      workspaceRoot = Paths.get(extract(WORKSPACE)),
      bazelBin = Paths.get(extract(BAZEL_BIN)),
      release = bazelReleaseVersion,
      isBzlModEnabled = starlarkSemantics.isBzlModEnabled,
      isWorkspaceEnabled = starlarkSemantics.isWorkspaceEnabled,
      externalAutoloads = starlarkSemantics.externalAutoloads,
    )
  }

  private data class StarlarkSemantics(
    val isBzlModEnabled: Boolean,
    val isWorkspaceEnabled: Boolean,
    val externalAutoloads: List<String>,
  )

  // Idea taken from https://github.com/bazelbuild/bazel/issues/21303#issuecomment-2007628330
  private fun parseStarlarkSemantics(starlarkSemantics: String, bazelReleaseVersion: BazelRelease): StarlarkSemantics {
    val isBzlModEnabled =
      when {
        "enable_bzlmod=true" in starlarkSemantics -> true
        "enable_bzlmod=false" in starlarkSemantics -> false
        else -> bazelReleaseVersion.major >= 7
      }
    val isWorkspaceEnabled =
      when {
        "enable_workspace=true" in starlarkSemantics -> true
        "enable_workspace=false" in starlarkSemantics -> false
        else -> bazelReleaseVersion.major <= 7
      }
    val autoloadsDisabled = "incompatible_disable_autoloads_in_main_repo=true" in starlarkSemantics

    // https://github.com/bazelbuild/bazel/issues/23043
    // https://bazel.build/reference/command-line-reference#flag--incompatible_autoload_externally
    val externalAutoloads = if (autoloadsDisabled) emptyList()
    else (
      parseExternalAutoloads(starlarkSemantics) ?: when (bazelReleaseVersion.major) {
        // Bazel 8 autoloads several rules by default to ease migration.
        // This can be turned off via e.g. --incompatible_autoload_externally=""
        8 -> listOf("rules_python", "rules_java", "rules_android")
        // Bazel 9 and higher will not autoload rules by default; Bazel 7 had bundled rules instead of autoloading.
        else -> emptyList()
      })

    return StarlarkSemantics(
      isBzlModEnabled = isBzlModEnabled,
      isWorkspaceEnabled = isWorkspaceEnabled,
      externalAutoloads = externalAutoloads,
    )
  }

  private fun parseExternalAutoloads(starlarkSemantics: String): List<String>? {
    val paramBegin = starlarkSemantics.indexOf(INCOMPATIBLE_AUTOLOADS_PARAM).takeIf { it != -1 } ?: return null
    val autoloadsBegin = paramBegin + INCOMPATIBLE_AUTOLOADS_PARAM.length
    val autoloadsEnd = starlarkSemantics.indexOf(']', startIndex = autoloadsBegin).takeIf { it != -1 } ?: return null
    val autoloadsText = starlarkSemantics.substring(autoloadsBegin until autoloadsEnd)

    return autoloadsText
      .split(',')
      .map {
        it.trim().removePrefix("+").removePrefix("@")
      }.filter {
        it.isNotEmpty()
      }
  }

  companion object {
    private val INFO_LINE_PATTERN = "([\\w-]+): (.*)".toRegex()
    private const val INCOMPATIBLE_AUTOLOADS_PARAM = "incompatible_autoload_externally=["
  }
}
