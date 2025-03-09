package org.jetbrains.bazel.android.run

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
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.includesAndroid

/**
 * Key for storing the target Android device inside an [ExecutionEnvironment]
 */
val DEVICE_FUTURE_KEY: Key<ListenableFuture<IDevice>> = Key.create("DEVICE_FUTURE_KEY")

class AndroidBazelRunHandler(private val configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.putUserData(DeployableToDevice.KEY, true)
    val provider = AndroidBeforeRunTaskProvider()
    val mobileInstallTask = provider.createTask(configuration)
    configuration.beforeRunTasks = listOf(mobileInstallTask)
  }

  override val state: AndroidBazelRunConfigurationState = AndroidBazelRunConfigurationState

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

    val androidConfigurationExecutor = BazelAndroidConfigurationExecutor(environment)
    return AndroidConfigurationExecutorRunProfileState(androidConfigurationExecutor)
  }

  class AndroidBspRunHandlerProvider : RunHandlerProvider {
    override val id: String = "AndroidBspRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = AndroidBazelRunHandler(configuration)

    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      BazelFeatureFlags.isAndroidSupportEnabled &&
        targetInfos.singleOrNull()?.let { it.languageIds.includesAndroid() && !it.capabilities.canTest } ?: false

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
  }
}
