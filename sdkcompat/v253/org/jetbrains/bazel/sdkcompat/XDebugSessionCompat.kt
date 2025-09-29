package org.jetbrains.bazel.sdkcompat

import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.xdebugger.XDebugSession

abstract class XDebugSessionCompat : XDebugSession {
  abstract fun isMixedModeCompat(): Boolean

  override fun isMixedMode(): Boolean = isMixedModeCompat()

  override fun getExecutionEnvironment(): ExecutionEnvironment? = null
}
