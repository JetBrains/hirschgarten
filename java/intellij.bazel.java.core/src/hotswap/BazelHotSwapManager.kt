package org.jetbrains.bazel.hotswap

import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.ui.HotSwapStatusListener
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

/**
 * Manages hotswapping for bazel java_binary run configurations.
 *
 * Customers could override the default implementation to add their telemetry, see BAZEL-3091
 */
interface BazelHotSwapManager {
  class HotSwapEnvironment(
    val oldManifest: JarFileManifest,
    val newManifest: JarFileManifest,
    val listener: HotSwapStatusListener?,
    val sessions: List<DebuggerSession>,
    val isAutoRun: Boolean,
  )

  suspend fun hotswap(environment: HotSwapEnvironment)

  fun getCurrentDebugSessions(): List<DebuggerSession>

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelHotSwapManager = project.service<BazelHotSwapManager>()
  }
}
