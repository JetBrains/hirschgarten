package org.jetbrains.plugins.bsp.android.run

import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key
import kotlinx.coroutines.runBlocking
import org.jetbrains.bsp.protocol.MobileInstallStartType
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration

private val PROVIDER_ID = Key.create<MobileInstallBeforeRunTaskProvider.Task>("MobileInstallBeforeRunTaskProvider")

public class MobileInstallBeforeRunTaskProvider : BeforeRunTaskProvider<MobileInstallBeforeRunTaskProvider.Task>() {
  private val log = logger<MobileInstallBeforeRunTaskProvider>()

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
    if (runConfiguration.runHandler !is AndroidBspRunHandler) return false

    val targetId = runConfiguration.target?.id?.toBsp4JTargetIdentifier() ?: return false
    val deviceFuture = environment.getCopyableUserData(DEVICE_FUTURE_KEY) ?: return false

    val startType = when (environment.executor.id) {
      DefaultRunExecutor.EXECUTOR_ID -> MobileInstallStartType.COLD
      DefaultDebugExecutor.EXECUTOR_ID -> MobileInstallStartType.DEBUG
      else -> return false
    }

    val mobileInstallResult = runBlocking {
      runMobileInstallTargetTask(targetId, deviceFuture, startType, environment.project, log)
    }
    return mobileInstallResult?.statusCode == StatusCode.OK
  }
}
