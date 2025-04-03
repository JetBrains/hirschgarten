package org.jetbrains.bazel.android.run

import com.android.ddmlib.IDevice
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.guava.await
import org.jetbrains.android.sdk.AndroidSdkUtils
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.server.tasks.BspTaskStatusLogger
import org.jetbrains.bazel.ui.console.ConsoleService
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.MobileInstallStartType
import org.jetbrains.bsp.protocol.StatusCode
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.cancellation.CancellationException

class MobileInstallTargetTask(
  private val project: Project,
  private val deviceFuture: ListenableFuture<IDevice>,
  private val startType: MobileInstallStartType,
) {
  private val log = logger<MobileInstallTargetTask>()

  suspend fun executeWithServer(server: JoinedBuildServer, targetId: Label): MobileInstallResult {
    val bspBuildConsole = ConsoleService.getInstance(project).buildConsole
    val originId = "mobile-install-" + UUID.randomUUID().toString()
    val cancelOn = CompletableFuture<Void>()

    startMobileInstallConsoleTask(targetId, deviceFuture, startType, bspBuildConsole, originId, cancelOn)
    val targetDeviceSerialNumber =
      getTargetAndroidDeviceSerialNumber(bspBuildConsole)
        ?: return MobileInstallResult(StatusCode.ERROR)
    val mobileInstallDeferred =
      BazelCoroutineService.getInstance(project).startAsync(lazy = true) {
        val mobileInstallParams = createMobileInstallParams(targetId, originId, targetDeviceSerialNumber)
        server.buildTargetMobileInstall(mobileInstallParams)
      }
    return BspTaskStatusLogger(mobileInstallDeferred, bspBuildConsole, originId) { statusCode }.getResult()
  }

  private fun startMobileInstallConsoleTask(
    targetId: Label,
    deviceFuture: ListenableFuture<IDevice>,
    startType: MobileInstallStartType,
    bspBuildConsole: TaskConsole,
    originId: String,
    cancelOn: CompletableFuture<Void>,
  ) {
    bspBuildConsole.startTask(
      originId,
      BazelPluginBundle.message("console.task.mobile.install.title"),
      BazelPluginBundle.message("console.task.mobile.install.in.progress.target", targetId.toShortString(project)),
      { cancelOn.cancel(true) },
    ) {
      BazelCoroutineService.getInstance(project).start {
        runMobileInstallTargetTask(targetId, deviceFuture, startType, project, log)
      }
    }
  }

  private fun createMobileInstallParams(
    targetId: Label,
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
            ?.toPath(),
      )
    return params
  }

  private suspend fun getTargetAndroidDeviceSerialNumber(bspBuildConsole: TaskConsole): String? {
    val launchedDevice = launchAndroidDevice(bspBuildConsole)
    return launchedDevice.serialNumber
  }

  private suspend fun launchAndroidDevice(bspBuildConsole: TaskConsole): IDevice {
    if (!deviceFuture.isDone) {
      bspBuildConsole.addMessage(BazelPluginBundle.message("console.task.mobile.install.waiting.for.target.device"))
    }
    return deviceFuture.await()
  }
}

suspend fun runMobileInstallTargetTask(
  targetId: Label,
  deviceFuture: ListenableFuture<IDevice>,
  startType: MobileInstallStartType,
  project: Project,
  log: Logger,
): MobileInstallResult? =
  try {
    saveAllFiles()
    withBackgroundProgress(project, BazelPluginBundle.message("console.task.mobile.install.in.progress")) {
      project.connection.runWithServer { MobileInstallTargetTask(project, deviceFuture, startType).executeWithServer(it, targetId) }
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
