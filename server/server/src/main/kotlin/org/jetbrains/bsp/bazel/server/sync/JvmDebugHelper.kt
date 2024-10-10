package org.jetbrains.bsp.bazel.server.sync

import org.jetbrains.bsp.bazel.server.model.Module
import org.jetbrains.bsp.bazel.server.model.isJvmLanguages
import org.jetbrains.bsp.protocol.RemoteDebugData

sealed interface JvmDebugType {
  data class UNKNOWN(val name: String) : JvmDebugType // debug type unknown

  data class JDWP(val port: Int) : JvmDebugType // used for Java and Kotlin

  companion object {
    fun fromDebugData(params: RemoteDebugData?): JvmDebugType? =
      when (params?.debugType?.lowercase()) {
        null -> null
        "jdwp" -> JDWP(params.port)
        else -> UNKNOWN(params.debugType)
      }
  }
}

object JvmDebugHelper {
  fun jdwpArgument(port: Int): String =
    // all used options are defined in https://docs.oracle.com/javase/8/docs/technotes/guides/jpda/conninv.html#Invocation
    "--wrapper_script_flag=--jvm_flag=-agentlib:jdwp=" +
      "transport=dt_socket," +
      "server=n," +
      "suspend=y," +
      "address=localhost:$port"

  fun generateRunArguments(debugType: JvmDebugType?): List<String> =
    when (debugType) {
      is JvmDebugType.JDWP -> listOf(jdwpArgument(debugType.port))
      else -> emptyList()
    }

  fun verifyDebugRequest(debugType: JvmDebugType?, moduleToRun: Module) =
    when (debugType) {
      null -> {
      } // not a debug request, nothing to check
      is JvmDebugType.JDWP ->
        if (!moduleToRun.isJvmLanguages()) {
          throw RuntimeException("JDWP debugging is only available for Java and Kotlin targets")
        } else {
        }

      is JvmDebugType.UNKNOWN -> throw RuntimeException("Unknown debug type: ${debugType.name}")
    }
}
