package org.jetbrains.bazel.jvm.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.jvm.run.ScriptPathBeforeRunTaskProvider.Task
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasBazelParams
import org.jetbrains.bazel.run.state.HasProgramArguments
import org.jetbrains.bazel.server.tasks.DefaultBuildTargetTask
import org.jetbrains.bazel.server.tasks.ScriptPathBuildTargetTask
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import java.nio.file.Path

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
    val runConfiguration = BazelRunConfiguration.get(environment)
    val isDebug = environment.executor is DefaultDebugExecutor
    val project = environment.project
    val scriptPath = scriptPathRef?.let { ScriptPathBuildTargetTask.createTempScriptFile() }
    val status =
      try {
        val buildTargetTask = if (scriptPath != null) {
          val state = runConfiguration.handler?.state
          val programArguments = transformProgramArguments((state as? HasProgramArguments)?.programArguments)
          val additionalBazelParams = transformProgramArguments((state as? HasBazelParams)?.additionalBazelParams)
          ScriptPathBuildTargetTask(
            scriptPath = scriptPath,
            programArguments = programArguments,
            additionalBazelParams = additionalBazelParams,
          )
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

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = BazelPluginBundle.message("console.task.build.title")

  class Task : BeforeRunTask<Task>(PROVIDER_ID)
}
