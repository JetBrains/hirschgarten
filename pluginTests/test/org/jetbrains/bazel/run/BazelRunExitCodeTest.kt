package org.jetbrains.bazel.run

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.testproject.buildServer
import org.jetbrains.bazel.test.framework.testproject.testBazelProjectFixture
import org.jetbrains.bazel.workspace.model.test.framework.BuildServerMock
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TestResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@BazelTestApplication
class BazelRunExitCodeTest {
  private val testBazel by testBazelProjectFixture()

  @Test
  fun `run configuration reports the exit code of a failed bazel run`() {
    testBazel.buildServer(BuildServerMock(runResult = RunResult(taskId = TaskGroupId.EMPTY.task("run"), exitCode = 1, stdout = null)))
    assertEquals(1, exitCode("GenericRunHandlerProvider", "//app:bin"))
  }

  @Test
  fun `test configuration reports the exit code of failed tests`() {
    testBazel.buildServer(BuildServerMock(testResult = TestResult(exitCode = 3)))
    assertEquals(3, exitCode("GenericTestHandlerProvider", "//app:test"))
  }

  private fun exitCode(providerId: String, label: String): Int {
    val project = testBazel.project
    val settings = RunManager.getInstance(project)
      .createConfiguration(label, runConfigurationType<BazelRunConfigurationType>().configurationFactories.first())
    val configuration = settings.configuration as BazelRunConfiguration
    configuration.updateRunProvider(listOf(Label.parse(label)), RunHandlerProvider.findRunHandlerProvider(providerId)!!)
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val runner = ProgramRunner.getRunner(executor.id, configuration)!!
    val environment = ExecutionEnvironmentBuilder.create(executor, configuration).runner(runner).build()
    val result = runInEdtAndGet {
      environment.state!!.execute(executor, runner)!!.also { it.processHandler.startNotify() }
    }
    try {
      assertTrue(result.processHandler.waitFor(60_000), "$label did not terminate")
      return result.processHandler.exitCode!!
    }
    finally {
      runInEdtAndWait { result.executionConsole?.let(Disposer::dispose) }
    }
  }
}
