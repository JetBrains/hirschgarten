package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.sdkcompat.KOTLIN_COROUTINE_LIB_KEY
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.TestParams
import java.util.concurrent.atomic.AtomicReference

class JvmTestHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.beforeRunTasks =
      listOfNotNull(
        KotlinCoroutineLibraryFinderBeforeRunTaskProvider().createTask(configuration),
      )
  }

  override val name: String
    get() = "Jvm Test Handler"

  override val state = JvmTestState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        environment.putCopyableUserData(KOTLIN_COROUTINE_LIB_KEY, AtomicReference())
        JvmTestWithDebugCommandLineState(environment, state)
      }

      else -> {
        BazelTestCommandLineState(environment, state)
      }
    }

  class JvmTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "JvmTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmTestHandler(configuration)

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      targetInfos.all {
        (it.kind.isJvmTarget() && it.kind.ruleType == RuleType.TEST)
      }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}

class JvmTestWithDebugCommandLineState(environment: ExecutionEnvironment, val settings: JvmTestState) :
  JvmDebuggableCommandLineState(environment, settings.debugPort) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelTestTaskListener(handler)

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override suspend fun startBsp(server: JoinedBuildServer, pidDeferred: CompletableDeferred<Long?>) {
    val configuration = environment.runProfile as BazelRunConfiguration
    val kotlinCoroutineLibParam = calculateKotlinCoroutineParams(environment, configuration.project).joinToString(" ")
    val additionalBazelParams = settings.additionalBazelParams ?: ""
    val testParams =
      TestParams(
        targets = configuration.targets,
        originId = originId.toString(),
        workingDirectory = settings.workingDirectory,
        arguments = transformProgramArguments(settings.programArguments),
        environmentVariables = settings.env.envs,
        debug = debugType,
        testFilter = settings.testFilter,
        additionalBazelParams = (additionalBazelParams + kotlinCoroutineLibParam).trim().ifEmpty { null },
      )

    BazelWorkspaceResolveService
      .getInstance(environment.project)
      .withEndpointProxy { it.buildTargetTest(testParams) }
  }
}
