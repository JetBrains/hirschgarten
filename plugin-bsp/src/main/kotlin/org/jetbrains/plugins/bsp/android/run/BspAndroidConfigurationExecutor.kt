package org.jetbrains.plugins.bsp.android.run

import com.android.ddmlib.IDevice
import com.android.ide.common.resources.AndroidManifestPackageNameUtils
import com.android.ide.common.util.PathString
import com.android.tools.idea.execution.common.AndroidConfigurationExecutor
import com.android.tools.idea.execution.common.debug.DebugSessionStarter
import com.android.tools.idea.execution.common.debug.impl.java.AndroidJavaDebugger
import com.android.tools.idea.execution.common.processhandler.AndroidProcessHandler
import com.android.tools.idea.model.AndroidModel
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
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.plugins.bsp.android.BspAndroidModel
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration

public class BspAndroidConfigurationExecutor(
  private val environment: ExecutionEnvironment,
) : AndroidConfigurationExecutor {
  override val configuration: RunConfiguration
    get() = environment.runProfile as RunConfiguration

  override fun run(indicator: ProgressIndicator): RunContentDescriptor = runBlockingCancellable {
    val packageName = getPackageNameOrThrow()
    val processHandler = AndroidProcessHandler(packageName, { it.forceStop(packageName) })
    val console = createConsole().apply {
      attachToProcess(processHandler)
    }

    val device = getDevice()
    processHandler.addTargetDevice(device)

    showLogcat(device, packageName)

    createRunContentDescriptor(processHandler, console, environment)
  }

  override fun debug(indicator: ProgressIndicator): RunContentDescriptor = runBlockingCancellable {
    val device = getDevice()
    val packageName = getPackageNameOrThrow()
    val debugSession = startDebugSession(device, packageName, environment, indicator)
    showLogcat(device, packageName)
    debugSession.runContentDescriptor
  }

  private suspend fun startDebugSession(
    device: IDevice,
    packageName: String,
    environment: ExecutionEnvironment,
    indicator: ProgressIndicator,
  ): XDebugSessionImpl {
    val debugger = AndroidJavaDebugger()
    val debuggerState = debugger.createState()

    return DebugSessionStarter.attachDebuggerToStartedProcess(
      device,
      packageName,
      environment,
      debugger,
      debuggerState,
      destroyRunningProcess = { d -> d.forceStop(packageName) },
      indicator,
    )
  }

  private fun getDevice(): IDevice {
    val deviceFuture = environment.getCopyableUserData(DEVICE_FUTURE_KEY)
      ?: throw ExecutionException("No target device")
    return deviceFuture.get()
  }

  private fun getPackageNameOrThrow(): String =
    getPackageName() ?: throw ExecutionException("Couldn't get package name from manifest")

  private fun getPackageName(): String? {
    val bspRunConfiguration = environment.runProfile as? BspRunConfiguration ?: return null
    val target = bspRunConfiguration.target ?: return null
    val module = ModuleManager.getInstance(environment.project).findModuleByName(target.id) ?: return null

    val androidFacet = AndroidFacet.getInstance(module) ?: return null
    val model = AndroidModel.get(androidFacet) as? BspAndroidModel ?: return null
    val sourceProvider = model.sourceProvider

    val manifest = sourceProvider.manifestFiles.singleOrNull()?.toNioPath() ?: return null
    val manifestPathString = PathString(manifest)

    return try {
      AndroidManifestPackageNameUtils.getPackageNameFromManifestFile(manifestPathString)
    } catch (_: Exception) {
      null
    }
  }

  private fun createConsole(): ConsoleView =
    TextConsoleBuilderFactory.getInstance().createBuilder(environment.project).console

  private fun showLogcat(device: IDevice, packageName: String) {
    environment.project.messageBus.syncPublisher(ShowLogcatListener.TOPIC).showLogcat(device, packageName)
  }

  // The "Apply changes" button isn't available for BspRunConfiguration, so this is an unexpected code path
  override fun applyChanges(indicator: ProgressIndicator): RunContentDescriptor {
    throw ExecutionException("Apply changes not supported")
  }

  override fun applyCodeChanges(indicator: ProgressIndicator): RunContentDescriptor {
    throw ExecutionException("Apply code changes not supported")
  }
}
