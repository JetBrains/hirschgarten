package org.jetbrains.bsp.protocol

sealed interface DebugType {
  data class JDWP(val port: Int) : DebugType // used for Java and Kotlin

  data class GoDlv(val port: Int) : DebugType // used for Go Delve Debugger
}
