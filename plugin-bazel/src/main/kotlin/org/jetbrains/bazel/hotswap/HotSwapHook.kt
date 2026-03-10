package org.jetbrains.bazel.hotswap

import com.intellij.debugger.impl.DebuggerSession
import com.intellij.openapi.extensions.ExtensionPointName

interface HotSwapHook {
  suspend fun onHotSwap(sessions: List<DebuggerSession>)

  companion object {
    internal val EP_NAME: ExtensionPointName<HotSwapHook> = ExtensionPointName.create("org.jetbrains.bazel.hotSwapHook")
  }
}
