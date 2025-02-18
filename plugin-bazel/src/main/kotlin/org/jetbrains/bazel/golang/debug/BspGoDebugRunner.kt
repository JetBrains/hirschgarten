package org.jetbrains.bazel.golang.debug

import com.goide.dlv.DlvDebugProcess
import com.goide.dlv.DlvDisconnectOption
import com.goide.dlv.DlvRemoteVmConnection
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jdom.Element
import org.jetbrains.bazel.run.config.BspRunConfiguration
import java.util.concurrent.atomic.AtomicReference

class BspGoDebugRunner : GenericProgramRunner<BspDebugRunnerSetting>() {
  override fun getRunnerId(): String = "BspGoDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    // if target cannot be debugged, do not offer debugging it
    if (executorId != DefaultDebugExecutor.EXECUTOR_ID) return false
    if (profile !is BspRunConfiguration) return false
    return profile.handler is GoBspRunHandler // todo: add test handler when implemented
  }

  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor {
    // cast should always succeed, because canRun(...) checks for a compatible profile
    return attachVM(state as GoDebuggableCommandLineState, environment)
  }

  private fun attachVM(state: GoDebuggableCommandLineState, executionEnvironment: ExecutionEnvironment): RunContentDescriptor {
    val ex = AtomicReference<ExecutionException>()
    val result = AtomicReference<RunContentDescriptor>()
    val project = executionEnvironment.project
    ApplicationManager.getApplication().invokeAndWait {
      try {
        val executionResult = state.execute(executionEnvironment.executor, this)
        result.set(
          XDebuggerManager
            .getInstance(project)
            .startSession(executionEnvironment, BspDebugProcessStarter(executionResult, state))
            .runContentDescriptor,
        )
      } catch (_: ProcessCanceledException) {
        // ignore
      } catch (e: ExecutionException) {
        ex.set(e)
      }
    }
    return ex.get()?.let { throw it } ?: result.get()
  }
}

class BspDebugRunnerSetting : RunnerSettings {
  override fun readExternal(element: Element?) {
    // empty settings, don't do anything
  }

  override fun writeExternal(element: Element?) {
    // empty settings, don't do anything
  }
}

private class BspDebugProcessStarter(private val executionResult: ExecutionResult, val state: GoDebuggableCommandLineState) :
  XDebugProcessStarter() {
  override fun start(session: XDebugSession): XDebugProcess {
    val sessionImpl = session as XDebugSessionImpl
    sessionImpl.addExtraActions(*executionResult.actions)
    (executionResult as? DefaultExecutionResult)?.let {
      sessionImpl.addRestartActions(*it.restartActions)
    }
    val connection = DlvRemoteVmConnection(DlvDisconnectOption.KILL)
    val remote = true
    val process = DlvDebugProcess(session, connection, executionResult, remote)
    connection.open(state.getDebugServerAddress())
    return process
  }
}
