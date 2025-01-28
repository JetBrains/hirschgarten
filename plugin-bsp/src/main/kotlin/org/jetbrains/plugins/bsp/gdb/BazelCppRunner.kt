package org.jetbrains.plugins.bsp.gdb

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.cidr.execution.CidrCommandLineState
import com.jetbrains.cidr.execution.CidrRunner


class BazelCppRunner : CidrRunner() {
  override fun getRunnerId(): String {
    return "BlazeCppAppRunner"
  }

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
   return true
  }

  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    if (environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID
      && state is CidrCommandLineState
    ) {
      val debugSession: XDebugSession = startDebugSession(state, environment, false)
      return debugSession.runContentDescriptor
    }
    return super.doExecute(state, environment)
  }
}
