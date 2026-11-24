package org.jetbrains.bazel.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.EDT
import com.intellij.terminal.TerminalExecutionConsoleBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.progress.ShowConsole
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.test.BazelTestConsoleProperties
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.server.tasks.runBuildConsoleTask
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId
import kotlin.random.Random

@ApiStatus.Internal
abstract class BazelCommandLineStateBase(environment: ExecutionEnvironment) : CommandLineState(environment) {
  protected val taskGroupId: TaskGroupId = TaskGroupId(environment.toString() + "-" + Random.nextBytes(8).toHexString())

  protected open val isIdBasedTestTree: Boolean get() = false

  protected open val usePty: Boolean get() = BazelFeatureFlags.usePty

  protected abstract fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener

  /** Run the command and return its exit code, or throw an exception if the server does not support running the configuration */
  protected abstract suspend fun runCommand(
    server: BazelServerFacade,
    pidDeferred: CompletableDeferred<Long?>,
    handler: BazelProcessHandler,
  ): Int

  final override fun startProcess(): BazelProcessHandler = doStartProcess(false)

  override fun createConsole(executor: Executor): ConsoleView? {
    if (!usePty) return super.createConsole(executor)
    return TerminalExecutionConsoleBuilder(environment.project)
      .convertLfToCrlfForProcessWithoutPty(true)
      .build()
  }

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
          runCommand(server, pid, handler)
        }
      }

    handler = if (runningTests)
      BazelTestProcessHandler(project, runDeferred, pid, isIdBasedTestTree, usePty)
    else
      BazelProcessHandler(project, runDeferred, pid, usePty)

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

  protected suspend fun withBuildConsoleTask(taskId: TaskId, command: suspend () -> RunResult): Int {
    var runResult: RunResult? = null
    runBuildConsoleTask(environment.project, taskId, BazelRunConfiguration.get(environment).targets, ShowConsole.ON_FAIL) {
      runResult = command()
      runResult.buildStatus
    }
    return runResult?.exitCode ?: 1
  }

  protected fun executeWithTestConsole(executor: Executor): ExecutionResult {
    val configuration = BazelRunConfiguration.get(environment)
    val properties = configuration.createTestConsoleProperties(executor)
    properties.isIdBasedTestTree = isIdBasedTestTree
    (properties as? BazelTestConsoleProperties)?.usePty = usePty
    val handler = doStartProcess(true)
    val console = SMTestRunnerConnectionUtil.createConsole(properties)
    console.attachToProcess(handler)
    val executionResult = DefaultExecutionResult(console, handler)
    properties.createRerunFailedTestsAction(console)?.let { executionResult.setRestartActions(it) }
    return executionResult
  }
}
