package org.jetbrains.bazel.fixtures

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.testFramework.common.waitUntil
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.assertions.assertNotNull
import org.jetbrains.bazel.assertions.findTarget
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.bazelRunConfigurationFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal val EXECUTION_TIMEOUT: Duration = 10.minutes

internal class BazelExecutionResult(
  val output: String,
  val exitCode: Int,
  val testsRoot: SMTestProxy.SMRootTestProxy?,
) {
  val leaves: List<SMTestProxy>
    get() = testsRoot?.allTests.orEmpty().filter { it.isLeaf }

  val testNames: List<String>
    get() = leaves.map { test -> test.parent?.takeUnless { it === testsRoot }?.let { "${it.name}.${test.name}" } ?: test.name }

  fun assertSuccess() {
    assertThat(exitCode).withFailMessage(output).isEqualTo(0)
  }
}

internal suspend fun Project.createBazelConfiguration(label: Label): BazelRunConfiguration {
  val settings = RunManager.getInstanceAsync(this).createConfiguration(label.toString(), bazelRunConfigurationFactory)
  val configuration = settings.configuration as BazelRunConfiguration
  val targets = listOf(findTarget(label))
  configuration.updateTargets(listOf(label), RunHandlerProvider.getRunHandlerProvider(this, targets))
  return configuration
}

internal suspend fun Project.executeBazelConfiguration(configuration: BazelRunConfiguration, executor: Executor): BazelExecutionResult =
  coroutineScope {
    withContext(Dispatchers.EDT) {
      val output = StringBuffer()
      val exitCode = CompletableDeferred<Int>()

      // register the listener first before starting the run config
      messageBus.connect(this@coroutineScope).subscribe(
        ExecutionManager.EXECUTION_TOPIC,
        object : ExecutionListener {
          override fun processStarting(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
            if (env.runProfile !== configuration) return
            handler.addProcessListener(
              object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                  if (ProcessOutputType.isStdout(outputType) && !event.text.startsWith("##teamcity[")) {
                    output.append(event.text)
                  }
                }
              },
            )
          }

          override fun processNotStarted(executorId: String, env: ExecutionEnvironment, cause: Throwable?) {
            if (env.runProfile !== configuration) return
            exitCode.completeExceptionally(cause ?: IllegalStateException("${configuration.name} did not start"))
          }

          override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, code: Int) {
            if (env.runProfile !== configuration) return
            exitCode.complete(code)
          }
        },
      )

      val runner = ProgramRunner.getRunner(executor.id, configuration).assertNotNull()
      val environment = ExecutionEnvironmentBuilder.create(executor, configuration).runner(runner).build()

      val deferredDescriptor = CompletableDeferred<RunContentDescriptor?>()
      environment.callback = ProgramRunner.Callback { deferredDescriptor.complete(it) }
      runner.execute(environment)
      val descriptor = deferredDescriptor.await().assertNotNull()
      val code = withTimeout(5.minutes) { exitCode.await() }

      val testsRoot = (descriptor.executionConsole as? SMTRunnerConsoleView)?.resultsViewer?.testsRootNode
      if (testsRoot != null) {
        waitUntil("the test tree of ${configuration.name} is complete") { !testsRoot.isInProgress }
      }

      return@withContext BazelExecutionResult(output.toString(), code, testsRoot)
    }
  }
