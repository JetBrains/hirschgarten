package org.jetbrains.bazel.sdkcompat

import com.intellij.xdebugger.XDebugSession

abstract class XDebugSessionCompat : XDebugSession {
  abstract fun isMixedModeCompat(): Boolean

  override fun isMixedMode(): Boolean = isMixedModeCompat()
}
