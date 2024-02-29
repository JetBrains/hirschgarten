package org.jetbrains.plugins.bsp.android.run

import com.android.ddmlib.IDevice
import com.android.tools.idea.execution.common.AndroidConfigurationExecutorRunProfileState
import com.android.tools.idea.execution.common.DeployableToDevice
import com.android.tools.idea.run.editor.DeployTargetContext
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.execution.BeforeRunTask
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesAndroid
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler

/**
 * Key for storing the target Android device inside an [ExecutionEnvironment]
 */
public val DEVICE_FUTURE_KEY: Key<ListenableFuture<IDevice>> = Key.create("DEVICE_FUTURE_KEY")

public class AndroidBspRunHandler : BspRunHandler {
  override fun canRun(target: BuildTargetInfo): Boolean =
    BspFeatureFlags.isAndroidSupportEnabled && target.languageIds.includesAndroid()

  override fun canDebug(target: BuildTargetInfo): Boolean = canRun(target)

  override fun prepareRunConfiguration(configuration: BspRunConfiguration) {
    configuration.putUserData(DeployableToDevice.KEY, true)
  }

  override fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    target: BuildTargetInfo,
  ): RunProfileState {
    val deployTargetContext = DeployTargetContext()
    val deployTarget = deployTargetContext.currentDeployTargetProvider.getDeployTarget(project)

    val deviceFutures = deployTarget.getDevices(project).get()
    if (deviceFutures.isEmpty()) {
      throw ExecutionException(BspPluginBundle.message("console.task.mobile.no.target.device"))
    } else if (deviceFutures.size > 1) {
      throw ExecutionException(BspPluginBundle.message("console.task.mobile.cannot.run.on.multiple.devices"))
    }

    // Store the device inside ExecutionEnvironment, so that MobileInstallBeforeRunTaskProvider can access it.
    // Not sure why this has to by copyable, but the Android plugin does so as well.
    environment.putCopyableUserData(DEVICE_FUTURE_KEY, deviceFutures.first())

    val androidConfigurationExecutor = BspAndroidConfigurationExecutor(environment)
    return AndroidConfigurationExecutorRunProfileState(androidConfigurationExecutor)
  }

  override fun getBeforeRunTasks(configuration: BspRunConfiguration): List<BeforeRunTask<*>> {
    val provider = MobileInstallBeforeRunTaskProvider()
    val mobileInstallTask = provider.createTask(configuration) ?: return emptyList()
    return listOf(mobileInstallTask)
  }
}
