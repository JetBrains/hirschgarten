package org.jetbrains.bazel.startup

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessUtil
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.bazelrunner.outputs.SpawnedProcess
import java.io.File

/**
 * Implementation of ProcessSpawner that uses IntelliJ's GeneralCommandLine
 */
object GenericCommandLineProcessSpawner : ProcessSpawner {
  private val LOG = Logger.getInstance(GenericCommandLineProcessSpawner::class.java)

  override suspend fun spawnProcess(
    command: List<String>,
    environment: Map<String, String>,
    redirectErrorStream: Boolean,
    workDirectory: String?,
  ): SpawnedProcess {
    val commandLine = GeneralCommandLine(command)
    if (workDirectory != null) {
      commandLine.withWorkDirectory(File(workDirectory))
    }
    if (environment.isNotEmpty()) {
      commandLine.withEnvironment(environment)
    }

    commandLine.withRedirectErrorStream(redirectErrorStream)

    val process = commandLine.createProcess()
    return IntellijSpawnedProcess(process)
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
