package org.jetbrains.plugins.bsp.android.run

import com.android.ddmlib.IDevice
import com.android.tools.idea.execution.common.AndroidConfigurationExecutor
import com.android.tools.idea.execution.common.debug.DebugSessionStarter
import com.android.tools.idea.execution.common.debug.impl.java.AndroidJavaDebugger
import com.android.tools.idea.execution.common.processhandler.AndroidProcessHandler
import com.android.tools.idea.run.ShowLogcatListener
import com.android.tools.idea.run.configuration.execution.createRunContentDescriptor
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.utils.findModuleNameProvider
import org.jetbrains.plugins.bsp.utils.orDefault
import com.android.tools.idea.project.getPackageName as getApplicationIdFromManifest

public class BspAndroidConfigurationExecutor(
  private val environment: ExecutionEnvironment,
) : AndroidConfigurationExecutor {
  override val configuration: RunConfiguration
    get() = environment.runProfile as RunConfiguration

  override fun run(indicator: ProgressIndicator): RunContentDescriptor = runBlockingCancellable {
    val applicationId = getApplicationIdOrThrow()
    val processHandler = AndroidProcessHandler(applicationId, { it.forceStop(applicationId) })
    val console = createConsole().apply {
      attachToProcess(processHandler)
    }

    val device = getDevice()
    processHandler.addTargetDevice(device)

    showLogcat(device, applicationId)

    createRunContentDescriptor(processHandler, console, environment)
  }

  override fun debug(indicator: ProgressIndicator): RunContentDescriptor = runBlockingCancellable {
    val device = getDevice()
    val applicationId = getApplicationIdOrThrow()
    val debugSession = startDebugSession(device, applicationId, environment, indicator)
    showLogcat(device, applicationId)
    debugSession.runContentDescriptor
  }

  private suspend fun startDebugSession(
    device: IDevice,
    applicationId: String,
    environment: ExecutionEnvironment,
    indicator: ProgressIndicator,
  ): XDebugSessionImpl {
    val debugger = AndroidJavaDebugger()
    val debuggerState = debugger.createState()

    return DebugSessionStarter.attachDebuggerToStartedProcess(
      device,
      applicationId,
      environment,
      debugger,
      debuggerState,
      destroyRunningProcess = { d -> d.forceStop(applicationId) },
      indicator,
    )
  }

  private fun getDevice(): IDevice {
    val deviceFuture = environment.getCopyableUserData(DEVICE_FUTURE_KEY)
      ?: throw ExecutionException("No target device")
    return deviceFuture.get()
  }

  private fun getApplicationIdOrThrow(): String =
    getApplicationId() ?: throw ExecutionException("Couldn't get application ID")

  private fun getApplicationId(): String? {
    val bspRunConfiguration = environment.runProfile as? BspRunConfiguration ?: return null
    val target = bspRunConfiguration.targets.singleOrNull() ?: return null
    val moduleNameProvider = environment.project.findModuleNameProvider().orDefault()
    val moduleName = moduleNameProvider(target)
    val module = ModuleManager.getInstance(environment.project).findModuleByName(moduleName) ?: return null
    return getApplicationIdFromManifest(module)
  }

  private fun createConsole(): ConsoleView =
    TextConsoleBuilderFactory.getInstance().createBuilder(environment.project).console

  private fun showLogcat(device: IDevice, applicationId: String) {
    environment.project.messageBus.syncPublisher(ShowLogcatListener.TOPIC).showLogcat(device, applicationId)
  }

  // The "Apply changes" button isn't available for BspRunConfiguration, so this is an unexpected code path
  override fun applyChanges(indicator: ProgressIndicator): RunContentDescriptor {
    throw ExecutionException("Apply changes not supported")
  }

  override fun applyCodeChanges(indicator: ProgressIndicator): RunContentDescriptor {
    throw ExecutionException("Apply code changes not supported")
  }
}
