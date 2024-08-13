package org.jetbrains.plugins.bsp.intellij

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration

public class IntellijPluginDebuggerRunner : GenericDebuggerRunner() {
  override fun getRunnerId(): String = "org.jetbrains.plugins.bsp.intellij.IntellijPluginDebuggerRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    if (executorId != DefaultDebugExecutor.EXECUTOR_ID) return false
    if (profile !is BspRunConfiguration) return false
    return profile.runHandler is IntellijPluginRunHandler
  }
}
