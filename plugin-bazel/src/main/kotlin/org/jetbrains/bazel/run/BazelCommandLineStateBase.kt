package org.jetbrains.bazel.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.EDT
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.sync.workspace.BazelEndpointProxy
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import java.util.UUID

abstract class BazelCommandLineStateBase(environment: ExecutionEnvironment) : CommandLineState(environment) {
  protected val originId: UUID = UUID.randomUUID()

  protected abstract fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener

  /** Run the actual BSP command or throw an exception if the server does not support running the configuration */
  protected abstract suspend fun startBsp(endpoints: BazelEndpointProxy, pidDeferred: CompletableDeferred<Long?>)

  final override fun startProcess(): BazelProcessHandler = doStartProcess(false)

  private fun doStartProcess(runningTests: Boolean): BazelProcessHandler {
    val configuration = environment.runProfile as BazelRunConfiguration
    val project = configuration.project

    val bazelCoroutineService = BazelCoroutineService.getInstance(project)

    lateinit var handler: BazelProcessHandler
    // We have to start runDeferred later, because we need to register the listener first
    // Otherwise, we might miss some events

    val pid = CompletableDeferred<Long?>()
    val runDeferred =
      bazelCoroutineService.startAsync(lazy = true) {
        BazelWorkspaceResolveService
          .getInstance(project)
          .withEndpointProxy {
            saveAllFiles()
            withContext(Dispatchers.EDT) {
              RunContentManager.getInstance(project).toFrontRunContent(environment.executor, handler)
            }
            startBsp(it, pid)
          }
      }

    handler =
      when (runningTests) {
        true -> BazelTestProcessHandler(project, runDeferred, pid)
        false -> BazelProcessHandler(project, runDeferred, pid)
      }
    val runListener = createAndAddTaskListener(handler)

    with(BazelTaskEventsService.getInstance(project)) {
      val originId = originId.toString()
      saveListener(originId, runListener)
      runDeferred.invokeOnCompletion {
        pid.complete(null)
        removeListener(originId)
      }
    }

    runDeferred.start()

    return handler
  }

  protected fun executeWithTestConsole(executor: Executor): ExecutionResult {
    val configuration = environment.runProfile as BazelRunConfiguration
    val properties = configuration.createTestConsoleProperties(executor)
    val handler = doStartProcess(true)

    val console: BaseTestsOutputConsoleView =
      SMTestRunnerConnectionUtil.createAndAttachConsole(
        BazelPluginBundle.message("console.tasks.test.framework.name"),
        handler,
        properties,
      )

    handler.notifyTextAvailable(ServiceMessageBuilder.testsStarted().toString() + "\n", ProcessOutputType.STDOUT)

    val actions = createActions(console, handler, executor)

    return DefaultExecutionResult(console, handler, *actions)
  }
}
