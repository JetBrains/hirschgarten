package org.jetbrains.bazel.python

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.ExecutionResult
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.python.debugger.PyDebugProcess
import com.jetbrains.python.debugger.PyDebugRunner
import com.jetbrains.python.run.PythonCommandLineState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.commons.label.label
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.server.tasks.runBuildTargetTask
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import java.net.ServerSocket
import kotlin.String

class PythonBspDebugRunner : PyDebugRunner() {
  override fun getRunnerId(): String = RUNNER_ID

  override fun canRun(executorId: String, profile: RunProfile): Boolean =
    executorId == DefaultDebugExecutor.EXECUTOR_ID &&
      profile is BspRunConfiguration &&
      profile.handler is PythonBspRunHandler &&
      profile.targets.size == 1 &&
      profile.project.basePath != null

  override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
    val debugState = state as? PythonDebugCommandLineState ?: error(BazelPluginBundle.message("python.debug.error.wrong.state"))
    val nativeState = debugState.asPythonState()
    val targetId = state.targetId ?: error(BazelPluginBundle.message("python.debug.error.no.id"))
    val promise = AsyncPromise<RunContentDescriptor?>()
    buildTarget(environment.project, targetId) {
      val superResult = ReadAction.compute<Promise<RunContentDescriptor?>, Throwable> { super.execute(environment, nativeState) }
      superResult.onSuccess { promise.setResult(it) }
      superResult.onError { promise.setError(it) }
    }
    return promise
  }

  private fun buildTarget(
    project: Project,
    targetId: BuildTargetIdentifier,
    onBuildComplete: () -> Unit,
  ) {
    BspCoroutineService.getInstance(project).start {
      runBuildTargetTask(listOf(targetId), project, log)
      withContext(Dispatchers.EDT) {
        onBuildComplete()
      }
    }
  }

  override fun createDebugProcess(
    session: XDebugSession,
    serverPort: Int,
    result: ExecutionResult?,
  ): PyDebugProcess = super.createDebugProcess(session, serverPort, result).withPositionConverter()

  override fun createDebugProcess(
    session: XDebugSession,
    serverSocket: ServerSocket?,
    result: ExecutionResult?,
    pyState: PythonCommandLineState?,
  ): PyDebugProcess = super.createDebugProcess(session, serverSocket, result, pyState).withPositionConverter()

  private fun PyDebugProcess.withPositionConverter(): PyDebugProcess =
    also {
      val targetId = (session.runProfile as BspRunConfiguration).targets.single() // canRun(...) checks that, should never fail
      it.positionConverter = BazelPyDebugPositionConverter(session.project, targetId.label())
    }

  companion object {
    private val log = logger<PythonBspDebugRunner>()
  }
}

private const val RUNNER_ID = "BspPythonDebugRunner"
