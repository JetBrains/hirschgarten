package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
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
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import java.util.concurrent.atomic.AtomicReference

class BazelJvmDebugRunner : GenericProgramRunner<BazelDebugRunnerSetting>() {
  override fun getRunnerId(): String = "BazelJvmDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    // if target cannot be debugged, do not offer debugging it
    if (executorId != DefaultDebugExecutor.EXECUTOR_ID) return false
    if (profile !is BazelRunConfiguration) return false
    return profile.handler is JvmRunHandler || profile.handler is JvmTestHandler
  }

  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor {
    // cast should always succeed, because canRun(...) checks for a compatible profile
    state as JvmDebuggableCommandLineState

    val ex = AtomicReference<ExecutionException>()
    val result = AtomicReference<RunContentDescriptor>()
    val project = environment.project
    ApplicationManager.getApplication().invokeAndWait {
      val debugEnvironment = state.createDebugEnvironment(environment)
      try {
        val debuggerSession =
          DebuggerManagerEx
            .getInstanceEx(project)
            .attachVirtualMachine(debugEnvironment)
            ?: error("VM attachment failed")
        val session = XDebuggerManager
          .getInstance(project)
          .startSession(environment, BspDebugProcessStarter(debuggerSession))
        result.set((session as XDebugSessionImpl).getMockRunContentDescriptor())
      } catch (_: ProcessCanceledException) {
        // ignore
      } catch (e: ExecutionException) {
        ex.set(e)
      }
    }
    return ex.get()?.let { throw it } ?: result.get()
  }
}

class BazelDebugRunnerSetting : RunnerSettings {
  override fun readExternal(element: Element?) {
    // empty settings, don't do anything
  }

  override fun writeExternal(element: Element?) {
    // empty settings, don't do anything
  }
}

private class BspDebugProcessStarter(private val debuggerSession: DebuggerSession) : XDebugProcessStarter() {
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
