package org.jetbrains.bsp.bazel.bazelrunner

import ch.epfl.scala.bsp4j.StatusCode
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.utils.BasicBazelInfo
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.bazelrunner.utils.LazyBazelInfo
import org.jetbrains.bsp.bazel.bazelrunner.utils.orLatestSupported
import org.jetbrains.bsp.bazel.commons.escapeNewLines
import java.nio.file.Paths

class BazelInfoResolver(private val bazelRunner: BazelRunner) {
  fun resolveBazelInfo(cancelChecker: CancelChecker): BazelInfo = LazyBazelInfo { bazelInfoFromBazel(cancelChecker) }

  private fun bazelInfoFromBazel(cancelChecker: CancelChecker): BazelInfo {
    val isBzlModEnabled = calculateBzlModEnabled(cancelChecker)
    val command = bazelRunner.buildBazelCommand { info() }
    val processResult =
      bazelRunner
        .runBazelCommand(command)
        .waitAndGetResult(cancelChecker, true)
    return parseBazelInfo(processResult, isBzlModEnabled)
  }

  private fun parseBazelInfo(bazelProcessResult: BazelProcessResult, isBzlModEnabled: Boolean): BasicBazelInfo {
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

    fun obtainBazelReleaseVersion() =
      BazelRelease.fromReleaseString(extract("release"))
        ?: bazelRunner.workspaceRoot?.let { BazelRelease.fromBazelVersionFile(it) }.orLatestSupported()

    return BasicBazelInfo(
      execRoot = extract("execution_root"),
      outputBase = Paths.get(extract("output_base")),
      workspaceRoot = Paths.get(extract("workspace")),
      release = obtainBazelReleaseVersion(),
      isBzlModEnabled = isBzlModEnabled,
    )
  }

  // this method does a small check whether bzlmod is enabled in the project
  // by running an arbitrary a bazel mod command and check for ok status code
  private fun calculateBzlModEnabled(cancelChecker: CancelChecker): Boolean =
    bazelRunner
      .buildBazelCommand { showRepo() }
      .let { bazelRunner.runBazelCommand(it, logProcessOutput = false) }
      .waitAndGetResult(cancelChecker)
      .statusCode == StatusCode.OK

  companion object {
    private val InfoLinePattern = "([\\w-]+): (.*)".toRegex()
  }
}
