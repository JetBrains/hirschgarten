package org.jetbrains.plugins.bsp.intellij

import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.server.tasks.runBuildTargetTask

private val PROVIDER_ID = Key.create<BuildPluginBeforeRunTaskProvider.Task>("BuildPluginBeforeRunTaskProvider")

public class BuildPluginBeforeRunTaskProvider : BeforeRunTaskProvider<BuildPluginBeforeRunTaskProvider.Task>() {
  private val log = logger<BuildPluginBeforeRunTaskProvider>()

  public class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = BspPluginBundle.message("console.task.build.title")

  override fun createTask(configuration: RunConfiguration): Task? =
    if (configuration is BspRunConfiguration) {
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
    val runConfiguration = environment.runProfile as? BspRunConfiguration ?: return false
    if (runConfiguration.handler !is IntellijPluginRunHandler) return false

    val targetIds = runConfiguration.targets
    val buildResult =
      runBlocking {
        runBuildTargetTask(targetIds, environment.project, log)
      }
    return buildResult?.statusCode == StatusCode.OK
  }
}
