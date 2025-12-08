package org.jetbrains.bazel.jvm.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtilRt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.jvm.run.ScriptPathBeforeRunTaskProvider.Task
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasBazelParams
import org.jetbrains.bazel.run.state.HasProgramArguments
import org.jetbrains.bazel.server.sync.DebugHelper
import org.jetbrains.bazel.server.tasks.BuildTargetTask
import org.jetbrains.bazel.server.tasks.DefaultBuildTargetTask
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bsp.protocol.DebugType
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RunParams
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

internal val SCRIPT_PATH_KEY: Key<Ref<Path>> = Key.create("bazel.jvm.script.path")

private const val PROVIDER_NAME = "BuildScriptBeforeRunTaskProvider"

private val PROVIDER_ID = Key.create<Task>(PROVIDER_NAME)

/**
 * As per [--script_path](https://bazel.build/reference/command-line-reference#run-flag--script_path) doc:
 * Writes a shell script to the given file which invokes the target.
 * Then the following can be used to invoke target `//foo`: `bazel run --script_path=foo //foo && ./foo`.
 * This differs from `bazel run //foo` in that the bazel lock is released allowing hotswap in tests.
 * This gets us:
 * - Diagnostics for a failed build
 * - Hotswap while debugging a test (because the lock is released)
 * - Because debugger is attached only once the ready script is run, it doesn't have to wait for the build (and possibly timeout)
 */
internal class ScriptPathBeforeRunTaskProvider : BeforeRunTaskProvider<Task>() {
  fun createTaskInstance(): Task = Task()

  override fun createTask(runConfiguration: RunConfiguration): Task? {
    if (runConfiguration !is BazelRunConfiguration) return null
    return createTaskInstance()
  }

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    // Skip for running with coverage, in which case SCRIPT_PATH_KEY is not set
    val scriptPathRef: Ref<Path>? = environment.getCopyableUserData(SCRIPT_PATH_KEY)
    val runConfiguration = environment.runProfile as BazelRunConfiguration
    val isDebug = environment.executor is DefaultDebugExecutor
    val project = environment.project
    val scriptPath = scriptPathRef?.let { createTempScriptFile() }
    val status =
      try {
        val buildTargetTask = if (scriptPath != null) {
          ScriptPathBuildTargetTask(runConfiguration, environment, scriptPath, isDebug)
        } else {
          /**
           * scriptPath is null when running tests without debug, which indicates that we should build but not pass --script_path
           * because --script_path screws up test result caching, but we still want diagnostics if the build fails
           */
          DefaultBuildTargetTask
        }
        // runBlocking instead of runBlockingCancellable because before run tasks aren't cancellable
        runBlocking {
          runBuildTargetTask(
            runConfiguration.targets,
            project,
            isDebug,
            buildTargetTask,
          )
        }
      } catch (_: CancellationException) {
        return false
      }
    if (status == BazelStatus.SUCCESS) {
      scriptPathRef?.set(scriptPath)
      return true
    } else {
      return false
    }
  }

  private fun createTempScriptFile(): Path =
    Files.createTempFile(Paths.get(FileUtilRt.getTempDirectory()), "bazel-script-", "").also { it.toFile().deleteOnExit() }

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = PROVIDER_NAME

  class Task : BeforeRunTask<Task>(PROVIDER_ID)
}

private class ScriptPathBuildTargetTask(
  private val runConfiguration: BazelRunConfiguration,
  private val environment: ExecutionEnvironment,
  private val scriptPath: Path,
  private val isDebug: Boolean,
) : BuildTargetTask {
  override suspend fun build(
    server: JoinedBuildServer,
    targetIds: List<Label>,
    buildConsole: TaskConsole,
    originId: String,
    debugFlags: List<String>,
  ): BazelStatus {
    val state = runConfiguration.handler?.state
    val debugPort = if (isDebug) {
      (state as? HasDebugPort)?.debugPort ?: 5005
    } else {
      null
    }
    val additionalBazelParams = transformProgramArguments((state as? HasBazelParams)?.additionalBazelParams)
    val programArguments = (state as? HasProgramArguments)?.programArguments

    val scriptPathParam = listOf("--script_path=$scriptPath")
    val additionalProgramArguments = debugPort?.let { DebugHelper.generateRunArguments(DebugType.JDWP(debugPort)) }
    val coroutineDebugParams = if (isDebug) retrieveKotlinCoroutineParams(environment, runConfiguration.project) else emptyList()
    val params =
      RunParams(
        target = targetIds.single(),
        originId = originId,
        arguments = programArguments?.let { transformProgramArguments(it) }.orEmpty() + additionalProgramArguments.orEmpty(),
        additionalBazelParams = (scriptPathParam + coroutineDebugParams + additionalBazelParams).joinToString(" "),
        /**
         * Environment variables are not written into the generated run script by Bazel, so they are handled by [JvmDebuggableCommandLineState].
         */
        environmentVariables = null,
      )
    return server.buildTargetRun(params).statusCode
  }
}
