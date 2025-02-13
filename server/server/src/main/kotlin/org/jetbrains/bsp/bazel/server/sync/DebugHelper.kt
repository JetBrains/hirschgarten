package org.jetbrains.bsp.bazel.server.sync

import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.server.model.Module
import org.jetbrains.bsp.bazel.server.model.isJvmLanguages
import org.jetbrains.bsp.protocol.RemoteDebugData

sealed interface DebugType {
  data class UNKNOWN(val name: String) : DebugType // debug type unknown

  data class JDWP(val port: Int) : DebugType // used for Java and Kotlin

  data class GoDlv(val port: Int) : DebugType // used for Go Delve Debugger

  companion object {
    fun fromDebugData(params: RemoteDebugData?): DebugType? =
      when (params?.debugType?.lowercase()) {
        null -> null
        "jdwp" -> JDWP(params.port)
        "go_dlv" -> GoDlv(params.port)
        else -> UNKNOWN(params.debugType)
      }
  }
}

object DebugHelper {
  fun jdwpArgument(port: Int): String =
    // all used options are defined in https://docs.oracle.com/javase/8/docs/technotes/guides/jpda/conninv.html#Invocation
    "--wrapper_script_flag=--jvm_flag=-agentlib:jdwp=" +
      "transport=dt_socket," +
      "server=n," +
      "suspend=y," +
      "address=localhost:$port"

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

  fun buildBeforeRun(debugType: DebugType?): Boolean = debugType !is DebugType.GoDlv

  fun verifyDebugRequest(debugType: DebugType?, moduleToRun: Module) =
    when (debugType) {
      null -> {
      } // not a debug request, nothing to check
      is DebugType.JDWP ->
        if (!moduleToRun.isJvmLanguages()) {
          throw RuntimeException("JDWP debugging is only available for Java and Kotlin targets")
        } else {
        }
      is DebugType.GoDlv -> {}
      is DebugType.UNKNOWN -> throw RuntimeException("Unknown debug type: ${debugType.name}")
    }
}
