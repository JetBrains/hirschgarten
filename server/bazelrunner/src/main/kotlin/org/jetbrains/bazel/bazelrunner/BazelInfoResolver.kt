package org.jetbrains.bazel.bazelrunner

import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bazel.bazelrunner.utils.orLatestSupported
import java.nio.file.Paths

private const val RELEASE = "release"
private const val EXECUTION_ROOT = "execution_root"
private const val OUTPUT_BASE = "output_base"
private const val WORKSPACE = "workspace"
private const val BAZEL_BIN = "bazel-bin"
private const val STARLARK_SEMANTICS = "starlark-semantics"

class BazelInfoResolver(private val bazelRunner: BazelRunner) {
  suspend fun resolveBazelInfo(): BazelInfo = bazelInfoFromBazel()

  private suspend fun bazelInfoFromBazel(): BazelInfo {
    val command =
      bazelRunner.buildBazelCommand {
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
          InfoLinePattern.matchEntire(line)?.let { it.groupValues[1] to it.groupValues[2] }
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

    // Idea taken from https://github.com/bazelbuild/bazel/issues/21303#issuecomment-2007628330
    val starlarkSemantics = extract(STARLARK_SEMANTICS)
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

    return BazelInfo(
      execRoot = extract(EXECUTION_ROOT),
      outputBase = Paths.get(extract(OUTPUT_BASE)),
      workspaceRoot = Paths.get(extract(WORKSPACE)),
      bazelBin = Paths.get(extract(BAZEL_BIN)),
      release = bazelReleaseVersion,
      isBzlModEnabled = isBzlModEnabled,
      isWorkspaceEnabled = isWorkspaceEnabled,
    )
  }

  companion object {
    private val InfoLinePattern = "([\\w-]+): (.*)".toRegex()
  }
}
