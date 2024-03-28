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
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.includesAndroid
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler

/**
 * Key for storing the target Android device inside an [ExecutionEnvironment]
 */
public val DEVICE_FUTURE_KEY: Key<ListenableFuture<IDevice>> = Key.create("DEVICE_FUTURE_KEY")

public class AndroidBspRunHandler : BspRunHandler {
  override fun canRun(targets: List<BuildTargetInfo>): Boolean =
    BspFeatureFlags.isAndroidSupportEnabled && targets.size == 1 && targets.all {
      it.languageIds.includesAndroid() && !it.capabilities.canTest
    }

  override fun canDebug(targets: List<BuildTargetInfo>): Boolean = canRun(targets)

  override fun prepareRunConfiguration(configuration: BspRunConfigurationBase) {
    configuration.putUserData(DeployableToDevice.KEY, true)
  }

  override fun getRunProfileState(
    project: Project,
    executor: Executor,
    environment: ExecutionEnvironment,
    configuration: BspRunConfigurationBase,
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

  override fun getBeforeRunTasks(configuration: BspRunConfigurationBase): List<BeforeRunTask<*>> {
    val provider = MobileInstallBeforeRunTaskProvider()
    val mobileInstallTask = provider.createTask(configuration) ?: return emptyList()
    return listOf(mobileInstallTask)
  }
}
