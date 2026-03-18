package com.intellij.bazel.devkit.monorepo.hotswap

import com.intellij.debugger.impl.DebuggerSession
import com.intellij.monorepo.devkit.bazel.hotswap.MonorepoDevkitHotswap
import org.jetbrains.bazel.hotswap.HotSwapHook

internal class MonorepoDevkitHotSwapHook : HotSwapHook {
  override suspend fun onHotSwap(sessions: List<DebuggerSession>) {
    val project = sessions.firstOrNull()?.project ?: return
    val monorepoDevkitHotswap = MonorepoDevkitHotswap.getInstance(project)
    monorepoDevkitHotswap.resetIntellijClassloaders(sessions)
  }
}
