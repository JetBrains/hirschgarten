package org.jetbrains.bazel.golang.debug

import com.goide.dlv.DlvDebugProcess
import com.goide.dlv.DlvDisconnectOption
import com.goide.dlv.DlvRemoteVmConnection
import com.goide.execution.GoBuildingRunner
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.resolvedPromise

class BazelGoDebugRunner : GoBuildingRunner() {
  override fun getRunnerId(): String = "BazelGoDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    // if target cannot be debugged, do not offer debugging it
    if (executorId != DefaultDebugExecutor.EXECUTOR_ID) return false
    if (profile !is BazelRunConfiguration) return false
    return profile.handler is BazelGoRunHandler || profile.handler is BazelGoTestHandler
  }

  // override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor {
  //  // cast should always succeed, because canRun(...) checks for a compatible profile
  //  return attachVM(state as GoDebuggableCommandLineState, environment)
  // }

  override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> =
    resolvedPromise(attachVM(state as GoDebuggableCommandLineState, environment))

  private fun attachVM(state: GoDebuggableCommandLineState, executionEnvironment: ExecutionEnvironment): RunContentDescriptor {
    val project = executionEnvironment.project
    state.patchNativeState()
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      { ReadAction.run<RuntimeException>(state::prepareStateInBGT) },
      BazelPluginBundle.message("go.debug.progress.manager.preparing.process.title"),
      false,
      project,
    )
    return XDebuggerManager
      .getInstance(project)
      .startSession(executionEnvironment, BazelDebugProcessStarter(state.execute(executionEnvironment.executor, this), state))
      .runContentDescriptor
  }
}

private class BazelDebugProcessStarter(private val executionResult: ExecutionResult, val state: GoDebuggableCommandLineState) :
  XDebugProcessStarter() {
  override fun start(session: XDebugSession): XDebugProcess {
    val sessionImpl = session as XDebugSessionImpl
    sessionImpl.addExtraActions(*executionResult.actions)
    (executionResult as? DefaultExecutionResult)?.let {
      sessionImpl.addRestartActions(*it.restartActions)
    }
    val connection = DlvRemoteVmConnection(DlvDisconnectOption.KILL)
    val process = DlvDebugProcess(session, connection, executionResult, true)
    connection.open(state.debugServerAddress)
    return process
  }
}
