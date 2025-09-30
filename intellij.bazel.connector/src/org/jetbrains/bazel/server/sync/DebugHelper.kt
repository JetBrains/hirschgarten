package org.jetbrains.bazel.server.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.protocol.DebugType

@ApiStatus.Internal
object DebugHelper {
  fun jdwpArgument(port: Int): String =
    // https://bazel.build/reference/command-line-reference#flag--java_debug
    "--wrapper_script_flag=--debug=$port"

  fun generateRunArguments(debugType: DebugType?): List<String> =
    when (debugType) {
      is DebugType.JDWP -> listOf(jdwpArgument(debugType.port))
      else -> emptyList()
    }

  fun generateRunOptions(debugType: DebugType?): List<String> =
    when (debugType) {
      is DebugType.GoDlv ->
        listOf(
          BazelFlag.runUnder(
            "dlv --listen=127.0.0.1:${debugType.port} --headless=true --api-version=2 --check-go-version=false --only-same-user=false exec",
          ),
          "--compilation_mode=dbg",
          "--dynamic_mode=off",
        )
      else -> emptyList()
    }

  /**
   * Common Bazel options we want to apply for any debug execution (run or test).
   * Currently forces local test strategy to make debugging reliable.
   */
  fun commonDebugBazelOptions(debugType: DebugType?): List<String> =
    if (debugType != null) listOf("--strategy=TestRunner=standalone") else emptyList()

  fun buildBeforeRun(debugType: DebugType?): Boolean = debugType !is DebugType.GoDlv
}
