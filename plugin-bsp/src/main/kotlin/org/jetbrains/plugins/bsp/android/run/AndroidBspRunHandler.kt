package org.jetbrains.plugins.bsp.android.run

import com.android.ddmlib.IDevice
import com.android.tools.idea.execution.common.AndroidConfigurationExecutorRunProfileState
import com.android.tools.idea.execution.common.DeployableToDevice
import com.android.tools.idea.run.editor.DeployTargetContext
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.includesAndroid
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

/**
 * Key for storing the target Android device inside an [ExecutionEnvironment]
 */
val DEVICE_FUTURE_KEY: Key<ListenableFuture<IDevice>> = Key.create("DEVICE_FUTURE_KEY")

class AndroidBspRunHandler(private val configuration: BspRunConfiguration) : BspRunHandler {
  init {
    configuration.putUserData(DeployableToDevice.KEY, true)
    val provider = MobileInstallBeforeRunTaskProvider()
    val mobileInstallTask = provider.createTask(configuration)
    configuration.beforeRunTasks = listOf(mobileInstallTask)
  }

  override val state: AndroidBspRunConfigurationState = AndroidBspRunConfigurationState

  override val name: String = "Android BSP Run Handler"

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val deployTargetContext = DeployTargetContext()
    val deployTarget = deployTargetContext.currentDeployTargetProvider.getDeployTarget(configuration.project)

    val deviceFutures = deployTarget.getDevices(configuration.project).get()
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

  class AndroidBspRunHandlerProvider : BspRunHandlerProvider {
    override val id: String = "AndroidBspRunHandlerProvider"

    override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = AndroidBspRunHandler(configuration)

    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      BspFeatureFlags.isAndroidSupportEnabled &&
        targetInfos.singleOrNull()?.let { it.languageIds.includesAndroid() && !it.capabilities.canTest } ?: false

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.all { it.capabilities.canDebug }
  }
}
