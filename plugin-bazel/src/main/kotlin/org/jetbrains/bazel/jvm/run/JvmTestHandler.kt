package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.engine.AsyncStacksUtils
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.DebugType
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.TestParams
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/** Used to store a runner to an [ExecutionEnvironment].  */
internal val KOTLIN_COROUTINE_LIB_KEY: Key<AtomicReference<String>> = Key.create("bazel.debug.kotlin.coroutine.lib")

class JvmTestHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.beforeRunTasks =
      listOfNotNull(
        KotlinCoroutineLibraryFinderBeforeRunTaskProvider().createTask(configuration),
      )
  }

  override val name: String = "Jvm Test Handler"

  override val state = JvmTestState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        environment.putCopyableUserData(KOTLIN_COROUTINE_LIB_KEY, AtomicReference())
        JvmTestWithDebugCommandLineState(environment, UUID.randomUUID().toString(), state)
      }

      else -> {
        BazelTestCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  class JvmTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "JvmTestHandlerProvider"

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

class JvmTestWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val settings: JvmTestState,
) : JvmDebuggableCommandLineState(environment, originId, settings.debugPort) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelTestTaskListener(handler)

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override suspend fun startBsp(server: JoinedBuildServer, pidDeferred: CompletableDeferred<Long?>) {
    val configuration = environment.runProfile as BazelRunConfiguration
    val targetIds = configuration.targets
    val kotlinCoroutineLibParam = calculateKotlinCoroutineParams(environment, configuration.project).joinToString(" ")
    val additionalBazelParams = settings.additionalBazelParams ?: ""
    val testParams =
      TestParams(
        targets = targetIds,
        originId = originId,
        workingDirectory = settings.workingDirectory,
        arguments = transformProgramArguments(settings.programArguments),
        environmentVariables = settings.env.envs,
        debug = DebugType.JDWP(getConnectionPort()),
        testFilter = settings.testFilter,
        additionalBazelParams = (additionalBazelParams + kotlinCoroutineLibParam).trim().ifEmpty { null },
      )

    BazelWorkspaceResolveService
      .getInstance(environment.project)
      .withEndpointProxy { it.buildTargetTest(testParams) }
  }
}

private fun createJvmFlag(flag: String) = "--jvmopt=$flag"

internal fun createJavaAgentFlag(jarPath: String) = createJvmFlag("-javaagent:$jarPath")

internal fun calculateKotlinCoroutineParams(environment: ExecutionEnvironment, project: Project): List<String> {
  val javaParameters = JavaParameters()
  AsyncStacksUtils.addDebuggerAgent(javaParameters, project, false)
  return listOfNotNull(
    environment.getCopyableUserData(KOTLIN_COROUTINE_LIB_KEY)?.get()?.let { createJavaAgentFlag(it) },
  ) + javaParameters.vmParametersList.parameters.map { createJvmFlag(it) }
}
