package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
sealed interface DebugType {
  data class JDWP(val port: Int) : DebugType // used for Java and Kotlin

  data class GoDlv(val port: Int) : DebugType // used for Go Delve Debugger
}
