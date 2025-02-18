package org.jetbrains.bazel.intellij

import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import org.jetbrains.bazel.run.config.BspRunConfiguration

public class IntellijPluginDebuggerRunner : GenericDebuggerRunner() {
  override fun getRunnerId(): String = "org.jetbrains.bazel.intellij.IntellijPluginDebuggerRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    if (executorId != DefaultDebugExecutor.EXECUTOR_ID) return false
    if (profile !is BspRunConfiguration) return false
    return profile.handler is IntellijPluginRunHandler
  }
}
