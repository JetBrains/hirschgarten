package org.jetbrains.bazel.jvm.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.sdkcompat.COROUTINE_JVM_FLAGS_KEY
import org.jetbrains.bazel.sdkcompat.calculateKotlinCoroutineParams
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.target.getModule
import org.jetbrains.bazel.target.targetUtils

private const val PROVIDER_NAME = "KotlinCoroutineLibraryFinderBeforeRunTaskProvider"

private val PROVIDER_ID = Key.create<KotlinCoroutineLibraryFinderBeforeRunTaskProvider.Task>(PROVIDER_NAME)

internal class KotlinCoroutineLibraryFinderBeforeRunTaskProvider :
  BeforeRunTaskProvider<KotlinCoroutineLibraryFinderBeforeRunTaskProvider.Task>() {
  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = PROVIDER_NAME

  class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun createTask(runConfiguration: RunConfiguration): Task? {
    if (runConfiguration !is BazelRunConfiguration) return null
    val project = runConfiguration.project
    if (!project.bazelJVMProjectSettings.enableKotlinCoroutineDebug) return null
    return Task()
  }

  /**
   * always return true for this task as it is just an add-on and should not block the debugging process
   */
  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    val runConfiguration = environment.runProfile as BazelRunConfiguration
    // skipping this task for non-debugging run config
    if (environment.executor !is DefaultDebugExecutor) return true
    val project = environment.project
    val target = runConfiguration.targets.single()
    val targetInfo = project.targetUtils.getBuildTargetForLabel(target) ?: return true
    if (!targetInfo.kind.includesKotlin() || !targetInfo.kind.isExecutable) return true
    val module = target.getModule(project) ?: return true
    runBlocking {
      withBackgroundProgress(project, BazelPluginBundle.message("background.task.description.preparing.for.debugging.kotlin", target)) {
        calculateKotlinCoroutineParams(environment, module)
      }
    }
    return true
  }
}

internal fun retrieveKotlinCoroutineParams(environment: ExecutionEnvironment, project: Project): List<String> {
  if (!project.bazelJVMProjectSettings.enableKotlinCoroutineDebug) return emptyList()
  return environment.getCopyableUserData(COROUTINE_JVM_FLAGS_KEY).get() ?: emptyList()
}
