package org.jetbrains.bazel.debug.platform

import com.intellij.xdebugger.breakpoints.XBreakpointProperties

// File path and line number are always preserved automatically, so they don't have to be kept here
// This class seems useless, but needs to be kept nevertheless to satisfy XLineBreakpointType's type parameter
class StarlarkBreakpointProperties : XBreakpointProperties<StarlarkBreakpointProperties>() {
  override fun getState(): StarlarkBreakpointProperties = this

  override fun loadState(state: StarlarkBreakpointProperties) = Unit
}
