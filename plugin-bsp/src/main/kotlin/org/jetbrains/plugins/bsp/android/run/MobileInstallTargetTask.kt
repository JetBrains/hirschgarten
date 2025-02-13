package org.jetbrains.plugins.bsp.android.run

import ch.epfl.scala.bsp4j.BuildServerCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import com.android.ddmlib.IDevice
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.future.await
import kotlinx.coroutines.guava.await
import org.jetbrains.android.sdk.AndroidSdkUtils
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.MobileInstallStartType
import org.jetbrains.plugins.bsp.action.saveAllFiles
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.server.tasks.BspServerSingleTargetTask
import org.jetbrains.plugins.bsp.server.tasks.BspTaskStatusLogger
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.cancellation.CancellationException

class MobileInstallTargetTask(
  project: Project,
  private val deviceFuture: ListenableFuture<IDevice>,
  private val startType: MobileInstallStartType,
) : BspServerSingleTargetTask<MobileInstallResult>("mobile install target", project) {
  private val log = logger<MobileInstallTargetTask>()

  override suspend fun executeWithServer(
    server: JoinedBuildServer,
    capabilities: BuildServerCapabilities,
    targetId: BuildTargetIdentifier,
  ): MobileInstallResult {
    val bspBuildConsole = BspConsoleService.getInstance(project).bspBuildConsole
    val originId = "mobile-install-" + UUID.randomUUID().toString()
    val cancelOn = CompletableFuture<Void>()

    startMobileInstallConsoleTask(targetId, deviceFuture, startType, bspBuildConsole, originId, cancelOn)
    val targetDeviceSerialNumber =
      getTargetAndroidDeviceSerialNumber(bspBuildConsole)
        ?: return MobileInstallResult(StatusCode.ERROR)
    val mobileInstallDeferred =
      BspCoroutineService.getInstance(project).startAsync(lazy = true) {
        val mobileInstallParams = createMobileInstallParams(targetId, originId, targetDeviceSerialNumber)
        server.buildTargetMobileInstall(mobileInstallParams).await()
      }
    return BspTaskStatusLogger(mobileInstallDeferred, bspBuildConsole, originId) { statusCode }.getResult()
  }

  private fun startMobileInstallConsoleTask(
    targetId: BuildTargetIdentifier,
    deviceFuture: ListenableFuture<IDevice>,
    startType: MobileInstallStartType,
    bspBuildConsole: TaskConsole,
    originId: String,
    cancelOn: CompletableFuture<Void>,
  ) {
    bspBuildConsole.startTask(
      originId,
      BspPluginBundle.message("console.task.mobile.install.title"),
      BspPluginBundle.message("console.task.mobile.install.in.progress.target", targetId.uri),
      { cancelOn.cancel(true) },
    ) {
      BspCoroutineService.getInstance(project).start {
        runMobileInstallTargetTask(targetId, deviceFuture, startType, project, log)
      }
    }
  }

  private fun createMobileInstallParams(
    targetId: BuildTargetIdentifier,
    originId: String,
    targetDeviceSerialNumber: String,
  ): MobileInstallParams {
    val params =
      MobileInstallParams(
        target = targetId,
        originId = originId,
        targetDeviceSerialNumber = targetDeviceSerialNumber,
        startType = startType,
        adbPath =
          AndroidSdkUtils
            .findAdb(project)
            .adbPath
            ?.toURI()
            ?.toString(),
      )
    return params
  }

  private suspend fun getTargetAndroidDeviceSerialNumber(bspBuildConsole: TaskConsole): String? {
    val launchedDevice = launchAndroidDevice(bspBuildConsole)
    return launchedDevice.serialNumber
  }

  private suspend fun launchAndroidDevice(bspBuildConsole: TaskConsole): IDevice {
    if (!deviceFuture.isDone) {
      bspBuildConsole.addMessage(BspPluginBundle.message("console.task.mobile.install.waiting.for.target.device"))
    }
    return deviceFuture.await()
  }
}

suspend fun runMobileInstallTargetTask(
  targetId: BuildTargetIdentifier,
  deviceFuture: ListenableFuture<IDevice>,
  startType: MobileInstallStartType,
  project: Project,
  log: Logger,
): MobileInstallResult? =
  try {
    saveAllFiles()
    withBackgroundProgress(project, BspPluginBundle.message("console.task.mobile.install.in.progress")) {
      MobileInstallTargetTask(project, deviceFuture, startType).connectAndExecute(targetId)
    }
  } catch (e: Exception) {
    when {
      e is CancellationException ->
        MobileInstallResult(StatusCode.CANCELLED)

      else -> {
        log.error(e)
        null
      }
    }
  }
