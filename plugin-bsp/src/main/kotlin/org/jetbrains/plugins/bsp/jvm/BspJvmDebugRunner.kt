package org.jetbrains.plugins.bsp.jvm

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.DefaultDebugEnvironment
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RemoteConnection
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
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import java.util.concurrent.atomic.AtomicReference

public class BspJvmDebugRunner : GenericProgramRunner<BspDebugRunnerSetting>() {
  override fun getRunnerId(): String = "BspJvmDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    // if target cannot be debugged, do not offer debugging it
    if (executorId != DefaultDebugExecutor.EXECUTOR_ID) return false
    if (profile !is BspRunConfiguration) return false
    return profile.runHandler is JvmBspRunHandler
  }

  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor {
    // cast should always succeed, because canRun(...) checks for it
    val debugState = state as JvmBspRunHandler.JvmDebugHandlerState
    val connection = debugState.remoteConnection
    return attachVM(state, environment, connection)
  }

  private fun attachVM(
    state: RunProfileState,
    executionEnvironment: ExecutionEnvironment,
    connection: RemoteConnection,
  ): RunContentDescriptor {
    val ex = AtomicReference<ExecutionException>()
    val result = AtomicReference<RunContentDescriptor>()
    val project = executionEnvironment.project
    ApplicationManager.getApplication().invokeAndWait {
      val debugEnvironment = DefaultDebugEnvironment(executionEnvironment, state, connection, 0L)
      try {
        val debuggerSession = DebuggerManagerEx.getInstanceEx(project)
          .attachVirtualMachine(debugEnvironment)
          ?: error("VM attachment failed")
        result.set(
          XDebuggerManager
            .getInstance(project)
            .startSession(executionEnvironment, BspDebugProcessStarter(debuggerSession))
            .runContentDescriptor
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

public class BspDebugRunnerSetting : RunnerSettings {
  override fun readExternal(element: Element?) {
    // empty settings, don't do anything
  }

  override fun writeExternal(element: Element?) {
    // empty settings, don't do anything
  }
}

private class BspDebugProcessStarter(
  private val debuggerSession: DebuggerSession,
) : XDebugProcessStarter() {
  override fun start(session: XDebugSession): XDebugProcess {
    val debugProcess = debuggerSession.process
    val sessionImpl = session as XDebugSessionImpl
    val executionResult = debugProcess.executionResult
    sessionImpl.addExtraActions(*executionResult.actions)
    (executionResult as? DefaultExecutionResult)?.let {
      sessionImpl.addRestartActions(*it.restartActions)
    }
    return JavaDebugProcess.create(session, debuggerSession)
  }
}
