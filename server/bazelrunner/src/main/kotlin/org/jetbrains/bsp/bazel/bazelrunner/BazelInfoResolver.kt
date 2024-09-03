package org.jetbrains.bsp.bazel.bazelrunner

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.utils.BasicBazelInfo
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.bazelrunner.utils.LazyBazelInfo
import org.jetbrains.bsp.bazel.bazelrunner.utils.orLatestSupported
import org.jetbrains.bsp.bazel.commons.escapeNewLines
import java.nio.file.Paths

private const val RELEASE = "release"
private const val EXECUTION_ROOT = "execution_root"
private const val OUTPUT_BASE = "output_base"
private const val WORKSPACE = "workspace"
private const val STARLARK_SEMANTICS = "starlark-semantics"

class BazelInfoResolver(private val bazelRunner: BazelRunner) {
  fun resolveBazelInfo(cancelChecker: CancelChecker): BazelInfo = LazyBazelInfo { bazelInfoFromBazel(cancelChecker) }

  private fun bazelInfoFromBazel(cancelChecker: CancelChecker): BazelInfo {
    val command = bazelRunner.buildBazelCommand {
      info {
        options.addAll(listOf(RELEASE, EXECUTION_ROOT, OUTPUT_BASE, WORKSPACE, STARLARK_SEMANTICS))
      }
    }
    val processResult =
      bazelRunner
        .runBazelCommand(command, serverPidFuture = null)
        .waitAndGetResult(cancelChecker, true)
    return parseBazelInfo(processResult)
  }

  private fun parseBazelInfo(bazelProcessResult: BazelProcessResult): BasicBazelInfo {
    val outputMap =
      bazelProcessResult
        .stdoutLines
        .mapNotNull { line ->
          InfoLinePattern.matchEntire(line)?.let { it.groupValues[1] to it.groupValues[2] }
        }.toMap()

    fun BazelProcessResult.meaningfulOutput() = if (isNotSuccess) stderr else stdout

    fun extract(name: String): String =
      outputMap[name]
        ?: error(
          "Failed to resolve $name from bazel info in ${bazelRunner.workspaceRoot}. " +
            "Bazel Info output: '${bazelProcessResult.meaningfulOutput().escapeNewLines()}'",
        )

    val bazelReleaseVersion =
      BazelRelease.fromReleaseString(extract(RELEASE))
        ?: bazelRunner.workspaceRoot?.let { BazelRelease.fromBazelVersionFile(it) }.orLatestSupported()

    // Idea taken from https://github.com/bazelbuild/bazel/issues/21303#issuecomment-2007628330
    val starlarkSemantics = extract(STARLARK_SEMANTICS)
    val isBzlModEnabled = if ("enable_bzlmod=true" in starlarkSemantics) {
      true
    } else if ("enable_bzlmod=false" in starlarkSemantics) {
      false
    } else {
      bazelReleaseVersion.major >= 7
    }

    return BasicBazelInfo(
      execRoot = extract(EXECUTION_ROOT),
      outputBase = Paths.get(extract(OUTPUT_BASE)),
      workspaceRoot = Paths.get(extract(WORKSPACE)),
      release = bazelReleaseVersion,
      isBzlModEnabled = isBzlModEnabled,
    )
  }

  companion object {
    private val InfoLinePattern = "([\\w-]+): (.*)".toRegex()
  }
}
