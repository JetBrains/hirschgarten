package org.jetbrains.bazel.debug.configuration

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
import com.intellij.xdebugger.XDebuggerManager
import org.jdom.Element
import java.util.concurrent.atomic.AtomicReference

class StarlarkDebugRunner : GenericProgramRunner<StarlarkDebugRunner.Settings>() {
  override fun getRunnerId(): String = "StarlarkDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean =
    executorId == DefaultDebugExecutor.EXECUTOR_ID &&
      profile is StarlarkDebugConfiguration

  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    // cast should always succeed - canRun(...) makes sure only StarlarkDebugConfiguration is run with this runner
    val starlarkState = state as StarlarkDebugConfigurationState
    val starlarkManager = starlarkState.manager
    val starter = starlarkManager.ProcessStarter()
    val project = environment.project
    val ex = AtomicReference<ExecutionException>()
    val result = AtomicReference<RunContentDescriptor>()

    ApplicationManager.getApplication().invokeAndWait {
      try {
        result.set(
          XDebuggerManager
            .getInstance(project)
            .startSession(environment, starter)
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

  class Settings : RunnerSettings {
    override fun readExternal(element: Element?) {
      // empty settings, don't do anything
    }

    override fun writeExternal(element: Element?) {
      // empty settings, don't do anything
    }
  }
}
