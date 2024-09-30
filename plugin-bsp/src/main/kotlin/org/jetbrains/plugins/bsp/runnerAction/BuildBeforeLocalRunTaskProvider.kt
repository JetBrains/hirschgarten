package org.jetbrains.plugins.bsp.runnerAction

import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import kotlinx.coroutines.runBlocking
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.impl.server.tasks.runBuildTargetTask
import org.jetbrains.plugins.bsp.runnerAction.LocalJvmRunnerAction.Companion.targetsToPreBuild

private val PROVIDER_ID = Key.create<BuildBeforeLocalRunTaskProvider.Task>("BuildBeforeLocalRunTaskProvider")

public class BuildBeforeLocalRunTaskProvider : BeforeRunTaskProvider<BuildBeforeLocalRunTaskProvider.Task>() {
  private val log = logger<BuildBeforeLocalRunTaskProvider>()

  public class Task : BeforeRunTask<Task>(PROVIDER_ID)

  override fun getId(): Key<Task> = PROVIDER_ID

  override fun getName(): String = BspPluginBundle.message("console.task.build.title")

  override fun createTask(configuration: RunConfiguration): Task? =
    if ((configuration as? UserDataHolderBase)?.getUserData(targetsToPreBuild)?.isNotEmpty() == true) {
      Task().apply { isEnabled = true }
    } else {
      null
    }

  override fun executeTask(
    context: DataContext,
    configuration: RunConfiguration,
    environment: ExecutionEnvironment,
    task: Task,
  ): Boolean {
    val targetIds = (configuration as? UserDataHolderBase)?.getUserData(targetsToPreBuild)?.takeUnless { it.isEmpty() } ?: return false
    val buildResult =
      runBlocking {
        runBuildTargetTask(targetIds, environment.project, log)
      }
    return buildResult?.statusCode == StatusCode.OK
  }
}
