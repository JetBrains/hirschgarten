package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.protocol.DebugType

object DebugHelper {
  fun jdwpArgument(port: Int): String =
    // https://bazel.build/reference/command-line-reference#flag--java_debug
    "--wrapper_script_flag=--debug=$port"

  fun spawnStrategy(): String =
    "--spawn_strategy=local"

  fun generateRunArguments(debugType: DebugType?): List<String> =
    when (debugType) {
      is DebugType.JDWP -> listOf(jdwpArgument(debugType.port), spawnStrategy())
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
}
