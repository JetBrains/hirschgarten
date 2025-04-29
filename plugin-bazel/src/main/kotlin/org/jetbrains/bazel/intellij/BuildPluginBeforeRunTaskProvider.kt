package org.jetbrains.bazel.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.server.tasks.runBuildTargetTask

private val PROVIDER_ID = Key.create<BuildPluginBeforeRunTaskProvider.Task>("BuildPluginBeforeRunTaskProvider")

public class BuildPluginBeforeRunTaskProvider : BeforeRunTaskProvider<BuildPluginBeforeRunTaskProvider.Task>() {
  private val log = logger<BuildPluginBeforeRunTaskProvider>()

  public class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = BazelPluginBundle.message("console.task.build.title")

  override fun createTask(configuration: RunConfiguration): Task? =
    if (configuration is BazelRunConfiguration) {
      Task()
    } else {
      null
    }

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    val runConfiguration = environment.runProfile as? BazelRunConfiguration ?: return false
    if (runConfiguration.handler !is IntellijPluginRunHandler) return false

    val targetIds = runConfiguration.targets
    val buildResult =
      runBlocking {
        runBuildTargetTask(targetIds, environment.project)
      }
    return buildResult?.statusCode == BazelStatus.SUCCESS
  }
}
