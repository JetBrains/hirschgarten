package org.jetbrains.bsp.bazel.bazelrunner

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.commons.escapeNewLines
import java.nio.file.Paths

class BazelInfoResolver(
    private val bazelRunner: BazelRunner,
    private val storage: BazelInfoStorage
) {

  fun resolveBazelInfo(cancelChecker: CancelChecker): BazelInfo {
    return LazyBazelInfo { storage.load() ?: bazelInfoFromBazel(cancelChecker) }
  }

  private fun bazelInfoFromBazel(cancelChecker: CancelChecker): BazelInfo {
    val processResult = bazelRunner.commandBuilder()
        .info().executeBazelCommand(useBuildFlags = false)
        .waitAndGetResult(cancelChecker,true)
    return parseBazelInfo(processResult).also { storage.store(it) }
  }

  private fun parseBazelInfo(bazelProcessResult: BazelProcessResult): BasicBazelInfo {
    val outputMap = bazelProcessResult
        .stdoutLines
        .mapNotNull { line ->
          InfoLinePattern.matchEntire(line)?.let { it.groupValues[1] to it.groupValues[2] }
        }.toMap()

    fun BazelProcessResult.meaningfulOutput() = if (isNotSuccess) stderr else stdout

    fun extract(name: String): String =
        outputMap[name]
            ?: error("Failed to resolve $name from bazel info in ${bazelRunner.workspaceRoot}. " +
                "Bazel Info output: '${bazelProcessResult.meaningfulOutput().escapeNewLines()}'")

    fun obtainBazelReleaseVersion() = BazelRelease.fromReleaseString(extract("release")) ?:
      bazelRunner.workspaceRoot?.let { BazelRelease.fromBazelVersionFile(it) }.orLatestSupported()

    return BasicBazelInfo(
        execRoot = extract("execution_root"),
        outputBase = Paths.get(extract("output_base")),
        workspaceRoot = Paths.get(extract("workspace")),
        release = obtainBazelReleaseVersion()
    )
  }

  companion object {
    private val InfoLinePattern = "([\\w-]+): (.*)".toRegex()
  }
}