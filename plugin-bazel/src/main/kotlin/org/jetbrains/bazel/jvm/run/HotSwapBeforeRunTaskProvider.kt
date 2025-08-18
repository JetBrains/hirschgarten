package org.jetbrains.bazel.jvm.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.hotswap.HotSwapUtils
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.server.sync.DebugHelper
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.notifications.BazelBalloonNotifier
import org.jetbrains.bsp.protocol.DebugType
import org.jetbrains.bsp.protocol.RunParams
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicReference

internal val SCRIPT_PATH_KEY: Key<AtomicReference<Path>> = Key.create("bazel.hotswap.jvm.script.path")

internal sealed class HotSwapBeforeRunTaskProvider<T : BeforeRunTask<T>> : BeforeRunTaskProvider<T>() {
  data class ExecutionParams(
    val arguments: String? = null,
    val environmentVariables: Map<String, String>? = null,
    val workingDirectory: String? = null,
    val additionalBazelParams: String? = null,
    val debugPort: Int = 5005,
  )

  abstract fun createTaskInstance(): T

  abstract fun executionParams(runConfiguration: BazelRunConfiguration): ExecutionParams

  override fun createTask(runConfiguration: RunConfiguration): T? {
    if (runConfiguration !is BazelRunConfiguration) return null
    val project = runConfiguration.project
    if (!HotSwapUtils.isHotSwapEligible(project)) return null
    return createTaskInstance()
  }

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: T,
  ): Boolean {
    val runConfiguration = environment.runProfile as BazelRunConfiguration
    // skipping this task for non-debugging run config
    if (environment.executor !is DefaultDebugExecutor) return true
    val scriptPath = createTempScriptFile()
    val scriptPathParam = listOf("--script_path=$scriptPath")
    val project = environment.project
    val targetUtils = project.targetUtils
    val targetInfos = runConfiguration.targets.mapNotNull { targetUtils.getBuildTargetForLabel(it) }
    if (targetInfos.any {
        !it.kind.isJvmTarget() || (!it.kind.isExecutable)
      }
    ) {
      return false
    }
    val target = runConfiguration.targets.single()

    val success =
      runBlocking {
        val executionParams = executionParams(runConfiguration)
        val additionalProgramArguments = DebugHelper.generateRunArguments(DebugType.JDWP(executionParams.debugPort))
        val additionalBazelParams = transformProgramArguments(executionParams.additionalBazelParams)
        val coroutineDebugParams = retrieveKotlinCoroutineParams(environment, configuration.project)
        val result =
          withBackgroundProgress(
            project,
            BazelPluginBundle.message("hotswap.before.run.script.path.background.progress.start.title", target),
          ) {
            val params =
              RunParams(
                target = target,
                originId = "",
                workingDirectory = executionParams.workingDirectory,
                arguments = executionParams.arguments?.let { transformProgramArguments(it) }.orEmpty() + additionalProgramArguments,
                environmentVariables = executionParams.environmentVariables,
                additionalBazelParams = (scriptPathParam + coroutineDebugParams + additionalBazelParams).joinToString(" "),
              )
            BazelServerService.getInstance(project)
              .connection
              .runWithServer { it.buildTargetRun(params) }
          }
        if (result.statusCode != BazelStatus.SUCCESS) {
          BazelBalloonNotifier.error(
            BazelPluginBundle.message("hotswap.before.run.script.path.generation.failure.title", target),
            BazelPluginBundle.message("hotswap.before.run.script.path.generation.failure.content", result.statusCode),
          )
          return@runBlocking false
        }
        environment.getCopyableUserData(SCRIPT_PATH_KEY).set(scriptPath)
        return@runBlocking true
      }
    return success
  }

  private fun createTempScriptFile(): Path =
    Files.createTempFile(Paths.get(FileUtilRt.getTempDirectory()), "bazel-script-", "").also { it.toFile().deleteOnExit() }
}

private const val TEST_PROVIDER_NAME = "HotswapTestBeforeRunTaskProvider"

private val TEST_PROVIDER_ID = Key.create<HotSwapTestBeforeRunTaskProvider.Task>(TEST_PROVIDER_NAME)

internal class HotSwapTestBeforeRunTaskProvider : HotSwapBeforeRunTaskProvider<HotSwapTestBeforeRunTaskProvider.Task>() {
  override fun createTaskInstance(): Task = Task()

  override fun executionParams(runConfiguration: BazelRunConfiguration): ExecutionParams {
    val state = runConfiguration.handler?.state as? JvmTestState ?: return ExecutionParams()
    return with(state) {
      ExecutionParams(
        arguments = programArguments,
        environmentVariables = env.envs,
        workingDirectory = workingDirectory,
        additionalBazelParams = additionalBazelParams,
        debugPort = debugPort,
      )
    }
  }

  override fun getId(): Key<Task> = TEST_PROVIDER_ID

  override fun getName(): String = TEST_PROVIDER_NAME

  class Task : BeforeRunTask<Task>(TEST_PROVIDER_ID)
}

private const val RUN_PROVIDER_NAME = "HotswapRunBeforeRunTaskProvider"

private val RUN_PROVIDER_ID = Key.create<HotSwapRunBeforeRunTaskProvider.Task>(RUN_PROVIDER_NAME)

internal class HotSwapRunBeforeRunTaskProvider : HotSwapBeforeRunTaskProvider<HotSwapRunBeforeRunTaskProvider.Task>() {
  override fun createTaskInstance(): Task = Task()

  override fun executionParams(runConfiguration: BazelRunConfiguration): ExecutionParams {
    val state = runConfiguration.handler?.state as? JvmRunState ?: return ExecutionParams()
    return with(state) {
      ExecutionParams(
        arguments = programArguments,
        environmentVariables = env.envs,
        workingDirectory = workingDirectory,
        additionalBazelParams = additionalBazelParams,
        debugPort = debugPort,
      )
    }
  }

  override fun getId(): Key<Task> = RUN_PROVIDER_ID

  override fun getName(): String = RUN_PROVIDER_NAME

  class Task : BeforeRunTask<Task>(RUN_PROVIDER_ID)
}
