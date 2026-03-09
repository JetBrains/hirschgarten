package org.jetbrains.bazel.debug.platform

import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import org.jetbrains.annotations.ApiStatus

// File paths and line numbers are already automatically held in XLineBreakpointImpl,
// which represents IntelliJ breakpoints - that is why they are not defined explicitly here.
// This class seems useless, but needs to be kept nevertheless to satisfy XLineBreakpointType's type parameter
@ApiStatus.Internal
class StarlarkBreakpointProperties : XBreakpointProperties<StarlarkBreakpointProperties>() {
  override fun getState(): StarlarkBreakpointProperties = this

  override fun loadState(state: StarlarkBreakpointProperties) = Unit
}
