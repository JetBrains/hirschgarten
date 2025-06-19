package org.jetbrains.bazel.startup

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.bazelrunner.outputs.SpawnedProcess
import java.io.File

/**
 * Implementation of ProcessSpawner that uses IntelliJ's GeneralCommandLine
 */
object GenericCommandLineProcessSpawner : ProcessSpawner {
  private val LOG = Logger.getInstance(GenericCommandLineProcessSpawner::class.java)

  override suspend fun spawnDeferredProcess(
    command: String,
    args: List<String>,
    environment: Map<String, String>,
    redirectErrorStream: Boolean,
    workDirectory: String?,
  ): Deferred<SpawnedProcess> {
    val deferred = CompletableDeferred<SpawnedProcess>()
    try {
      val processArgs = listOf(command) + args
      val commandLine = GeneralCommandLine(processArgs)
      if (workDirectory != null) {
        commandLine.withWorkDirectory(File(workDirectory))
      }
      if (environment.isNotEmpty()) {
        commandLine.withEnvironment(environment)
      }

      commandLine.withRedirectErrorStream(redirectErrorStream)

      val process = commandLine.createProcess()
      deferred.complete(IntellijSpawnedProcess(process))
    } catch (e: Throwable) {
      deferred.completeExceptionally(e)
    }
    return deferred
  }

  override fun killProcessTree(process: SpawnedProcess): Boolean =
    when (process) {
      is IntellijSpawnedProcess -> OSProcessUtil.killProcessTree(process.process)
      else -> {
        LOG.warn("Unknown process type: ${process.javaClass.name}")
        false
      }
    }

  override fun killProcess(process: SpawnedProcess) {
    when (process) {
      is IntellijSpawnedProcess -> OSProcessUtil.killProcess(process.process)
      else -> {
        LOG.warn("Unknown process type: ${process.javaClass.name}")
        killProcess(process.pid.toInt())
      }
    }
  }

  override fun killProcess(pid: Int) {
    OSProcessUtil.killProcess(pid)
  }
}
