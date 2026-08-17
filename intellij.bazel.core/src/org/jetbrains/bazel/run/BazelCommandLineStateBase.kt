package org.jetbrains.bazel.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.EDT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.TaskGroupId
import kotlin.random.Random

@ApiStatus.Internal
abstract class BazelCommandLineStateBase(environment: ExecutionEnvironment) : CommandLineState(environment) {
  protected val taskGroupId: TaskGroupId = TaskGroupId(environment.toString() + "-" + Random.nextBytes(8).toHexString())

  protected open val isIdBasedTestTree: Boolean get() = false

  protected open val testRunnerEmitsServiceMessages: Boolean get() = false

  protected abstract fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener

  protected open fun createTestRestartActions(console: SMTRunnerConsoleView): Array<AnAction> = emptyArray()

  /** Run the actual BSP command or throw an exception if the server does not support running the configuration */
  protected abstract suspend fun startBsp(
    server: BazelServerFacade,
    pidDeferred: CompletableDeferred<Long?>,
    handler: BazelProcessHandler,
  )

  final override fun startProcess(): BazelProcessHandler = doStartProcess(false)

  private fun doStartProcess(runningTests: Boolean): BazelProcessHandler {
    val configuration = BazelRunConfiguration.get(environment)
    val project = configuration.project

    val bazelCoroutineService = BazelCoroutineService.getInstance(project)

    lateinit var handler: BazelProcessHandler
    // We have to start runDeferred later, because we need to register the listener first
    // Otherwise, we might miss some events

    val pid = CompletableDeferred<Long?>()
    val runDeferred =
      bazelCoroutineService.startAsync(lazy = true) {
        project.connection.runWithServer { server: BazelServerFacade ->
          saveAllFiles()
          withContext(Dispatchers.EDT) {
            RunContentManager.getInstance(project).toFrontRunContent(environment.executor, handler)
          }
          startBsp(server, pid, handler)
        }
      }

    handler = if (runningTests)
      BazelTestProcessHandler(project, runDeferred, pid, isIdBasedTestTree)
    else
      BazelProcessHandler(project, runDeferred, pid)

    val runListener = createAndAddTaskListener(handler)

    with(BazelTaskEventsService.getInstance(project)) {
      saveListener(taskGroupId, runListener)
      runDeferred.invokeOnCompletion {
        pid.complete(null)
        removeListener(taskGroupId)
      }
    }

    runDeferred.start()

    return handler
  }

  protected fun executeWithTestConsole(executor: Executor): ExecutionResult {
    val configuration = BazelRunConfiguration.get(environment)
    val properties = configuration.createTestConsoleProperties(executor)
    properties.isIdBasedTestTree = isIdBasedTestTree
    val handler = doStartProcess(true)

    val console =
      SMTestRunnerConnectionUtil.createConsole(
        properties,
      )
    console.attachToProcess(handler)

    if (!testRunnerEmitsServiceMessages) {
      handler.notifyTextAvailable(ServiceMessageBuilder.testsStarted().toString() + "\n", ProcessOutputType.STDOUT)
    }

    val actions = createActions(console, handler, executor)

    val executionResult = DefaultExecutionResult(console, handler, *actions)
    val restartActions = createTestRestartActions(console)
    if (restartActions.isNotEmpty()) {
      executionResult.setRestartActions(*restartActions)
    }
    return executionResult
  }
}
