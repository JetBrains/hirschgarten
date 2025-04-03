package org.jetbrains.bazel.android.run

import com.android.ddmlib.IDevice
import com.android.tools.deployer.Deployer
import com.android.tools.idea.execution.common.AndroidConfigurationExecutor
import com.android.tools.idea.execution.common.DeployOptions
import com.android.tools.idea.execution.common.debug.DebugSessionStarter
import com.android.tools.idea.execution.common.debug.impl.java.AndroidJavaDebugger
import com.android.tools.idea.execution.common.deploy.deployAndHandleError
import com.android.tools.idea.execution.common.processhandler.AndroidProcessHandler
import com.android.tools.idea.execution.common.stats.RunStats
import com.android.tools.idea.projectsystem.ApplicationProjectContext
import com.android.tools.idea.run.ApkInfo
import com.android.tools.idea.run.ApkProvider
import com.android.tools.idea.run.ApkProvisionException
import com.android.tools.idea.run.ShowLogcatListener
import com.android.tools.idea.run.activity.DefaultStartActivityFlagsProvider
import com.android.tools.idea.run.activity.launch.DefaultActivityLaunch
import com.android.tools.idea.run.configuration.execution.ApplicationDeployerImpl
import com.android.tools.idea.run.configuration.execution.createRunContentDescriptor
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.workspaceModel.ide.toPath
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.target.getModuleEntity
import org.jetbrains.bazel.workspacemodel.entities.androidAddendumEntity
import java.nio.file.Path
import kotlin.io.path.exists

class BazelAndroidConfigurationExecutor(private val environment: ExecutionEnvironment) : AndroidConfigurationExecutor {
  override val configuration: RunConfiguration
    get() = environment.runProfile as RunConfiguration

  private val targetId: Label
    get() =
      (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
        ?: throw ExecutionException("Couldn't get BSP target from run configuration")

  override fun run(indicator: ProgressIndicator): RunContentDescriptor =
    runBlockingCancellable {
      val applicationId = getApplicationIdOrThrow()
      val processHandler = AndroidProcessHandler(applicationId, { it.forceStop(applicationId) })
      val console =
        createConsole().apply {
          attachToProcess(processHandler)
        }

      val device = getDevice()
      processHandler.addTargetDevice(device)
      if (!useMobileInstall()) {
        deployApk(environment, targetId, device, console, indicator)
      }
      showLogcat(device, applicationId)

      createRunContentDescriptor(processHandler, console, environment)
    }

  private fun useMobileInstall() =
    ((configuration as? BazelRunConfiguration)?.handler as? AndroidBazelRunHandler)?.state?.useMobileInstall ?: false

  override fun debug(indicator: ProgressIndicator): RunContentDescriptor =
    runBlockingCancellable {
      val device = getDevice()
      val applicationId = getApplicationIdOrThrow()
      val console = createConsole()
      if (!useMobileInstall()) {
        deployApk(environment, targetId, device, console, indicator)
      }
      val debugSession = startDebugSession(device, applicationId, environment, indicator, console)
      showLogcat(device, applicationId)
      debugSession.runContentDescriptor
    }

  private suspend fun startDebugSession(
    device: IDevice,
    applicationId: String,
    environment: ExecutionEnvironment,
    indicator: ProgressIndicator,
    console: ConsoleView,
  ): XDebugSessionImpl {
    val debugger = AndroidJavaDebugger()
    val debuggerState = debugger.createState()

    return DebugSessionStarter.attachDebuggerToStartedProcess(
      device,
      object : ApplicationProjectContext {
        override val applicationId: String = applicationId
      },
      environment,
      debugger,
      debuggerState,
      destroyRunningProcess = { d -> d.forceStop(applicationId) },
      indicator,
      console,
    )
  }

  private fun getDevice(): IDevice {
    val deviceFuture =
      environment.getCopyableUserData(DEVICE_FUTURE_KEY)
        ?: throw ExecutionException("No target device")
    return deviceFuture.get()
  }

  private fun getApplicationIdOrThrow(): String =
    try {
      BspApplicationIdProvider(environment.project, targetId).packageName
    } catch (e: ApkProvisionException) {
      throw ExecutionException(e.message)
    }

  private fun createConsole(): ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project).console

  private fun showLogcat(device: IDevice, applicationId: String) {
    environment.project.messageBus
      .syncPublisher(ShowLogcatListener.TOPIC)
      .showLogcat(device, applicationId)
  }

  // The "Apply changes" button isn't available for BspRunConfiguration, so this is an unexpected code path
  override fun applyChanges(indicator: ProgressIndicator): RunContentDescriptor = throw ExecutionException("Apply changes not supported")

  override fun applyCodeChanges(indicator: ProgressIndicator): RunContentDescriptor =
    throw ExecutionException("Apply code changes not supported")

  private fun deployApk(
    environment: ExecutionEnvironment,
    targetId: Label,
    device: IDevice,
    console: ConsoleView,
    indicator: ProgressIndicator,
  ) {
    val project = environment.project
    val apkPath =
      getApkPath(project, targetId)
        ?: throw ExecutionException(BazelPluginBundle.message("console.task.apk.install.could.not.get.apk"))

    val applicationId =
      try {
        BspApplicationIdProvider(project, targetId).packageName
      } catch (_: ApkProvisionException) {
        throw ExecutionException(BazelPluginBundle.message("console.task.apk.install.could.not.get.application.id"))
      }

    if (!apkPath.exists()) {
      throw ExecutionException(BazelPluginBundle.message("console.task.apk.install.apk.not.found", apkPath.toString()))
    }
    val apkInfo =
      ApkInfo(
        apkPath.toFile(),
        applicationId,
      )

    val deployResult =
      deployAndHandleError(
        env = environment,
        deployerAction = {
          listOf(deploy(project, environment, device, apkInfo, indicator))
        },
        automaticallyApplyResolutionAction = false,
      ).single()

    val isDebug = environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID
    val startActivityFlagsProvider = DefaultStartActivityFlagsProvider(project, isDebug, "")
    val flags = startActivityFlagsProvider.getFlags(device)
    val state = DefaultActivityLaunch.INSTANCE.createState()
    val apkProvider = ApkProvider { listOf(apkInfo) }
    state.launch(device, deployResult.app, apkProvider, isDebug, flags, console, RunStats.from(environment))
  }

  private fun getApkPath(project: Project, targetId: Label): Path? {
    val moduleEntity = targetId.getModuleEntity(project) ?: return null
    val androidAddendumEntity = moduleEntity.androidAddendumEntity ?: return null
    return androidAddendumEntity.apk?.toPath()
  }

  private fun deploy(
    project: Project,
    environment: ExecutionEnvironment,
    device: IDevice,
    apkInfo: ApkInfo,
    indicator: ProgressIndicator,
  ): Deployer.Result {
    val deployer = ApplicationDeployerImpl(project, RunStats.from(environment))
    val deployOptions =
      DeployOptions(
        disabledDynamicFeatures = emptyList(),
        pmInstallFlags = "",
        installOnAllUsers = true,
        alwaysInstallWithPm = true,
        allowAssumeVerified = true,
      )
    return deployer.fullDeploy(device, apkInfo, deployOptions, indicator)
  }
}
