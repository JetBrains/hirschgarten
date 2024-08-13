package org.jetbrains.plugins.bsp.run.commandLine

import ch.epfl.scala.bsp4j.TestParams
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.BazelTestParamsData
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.BspCommandLineStateBase
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.run.BspTaskListener
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericTestState
import org.jetbrains.plugins.bsp.run.task.BspTestTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import java.util.concurrent.CompletableFuture

class BspTestCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val state: GenericTestState,
) : BspCommandLineStateBase(environment, originId) {
  private val configuration = environment.runProfile as BspRunConfiguration

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val properties = configuration.createTestConsoleProperties(executor)
    val handler = startProcess()

    val console: BaseTestsOutputConsoleView =
      SMTestRunnerConnectionUtil.createAndAttachConsole(
        BspPluginBundle.message("console.tasks.test.framework.name"),
        handler,
        properties,
      )

    handler.notifyTextAvailable(ServiceMessageBuilder.testsStarted().toString(), ProcessOutputType.STDOUT)
    // OutputToGeneralTestEventsConverter.MyServiceMessageVisitor.visitServiceMessage
    //  ignores the first testingStarted event
    handler.notifyTextAvailable("\n##teamcity[testingStarted]\n", ProcessOutputType.STDOUT)
    handler.notifyTextAvailable("\n##teamcity[testingStarted]\n", ProcessOutputType.STDOUT)

    val actions = createActions(console, handler, executor)

    return DefaultExecutionResult(console, handler, *actions)
  }

  override fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener = BspTestTaskListener(handler)

  override fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities): CompletableFuture<*> {
    if (configuration.targets.isEmpty() || capabilities.testProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val targets = configuration.targets
    val params = TestParams(targets)
    params.originId = originId
    params.workingDirectory = state.workingDirectory
    params.arguments = transformProgramArguments(state.programArguments)
    params.environmentVariables = state.env.envs
    params.dataKind = BazelTestParamsData.DATA_KIND
    params.data = BazelTestParamsData(false, state.testFilter) // TODO: handle coverage
    return server.buildTargetTest(params)
  }
}
