package org.jetbrains.bazel.server.process

import com.intellij.util.io.awaitExit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.bazelrunner.outputs.SpawnedProcess
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Server-side implementation of ProcessSpawner that uses standard Java ProcessBuilder
 */
object ServerProcessSpawner : ProcessSpawner {
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
      val processBuilder = ProcessBuilder(processArgs)

      if (workDirectory != null) {
        processBuilder.directory(File(workDirectory))
      }

      if (environment.isNotEmpty()) {
        processBuilder.environment().putAll(environment)
      }

      if (redirectErrorStream) {
        processBuilder.redirectErrorStream(true)
      }

      val process = processBuilder.start()
      deferred.complete(ServerSpawnedProcess(process))
    } catch (e: Throwable) {
      deferred.completeExceptionally(e)
    }
    return deferred
  }

  override fun killProcessTree(process: SpawnedProcess): Boolean =
    when (process) {
      is ServerSpawnedProcess -> {
        try {
          process.process.destroyForcibly()
          true
        } catch (e: Exception) {
          false
        }
      }
      else -> {
        println("WARN: Unknown process type: ${process.javaClass.name}")
        false
      }
    }

  override fun killProcess(process: SpawnedProcess) {
    when (process) {
      is ServerSpawnedProcess -> process.process.destroyForcibly()
      else -> {
        println("WARN: Unknown process type: ${process.javaClass.name}")
        killProcess(process.pid.toInt())
      }
    }
  }

  override fun killProcess(pid: Int) {
    try {
      val killProcess = ProcessBuilder("kill", "-9", pid.toString()).start()
      killProcess.waitFor()
    } catch (e: Exception) {
      println("WARN: Failed to kill process $pid: ${e.message}")
    }
  }
}

/**
 * Server-side implementation of SpawnedProcess that wraps a standard Java Process
 */
class ServerSpawnedProcess(internal val process: Process) : SpawnedProcess {
  override val pid: Long = process.pid()
  override val inputStream: InputStream = process.inputStream
  override val errorStream: InputStream = process.errorStream
  override val isAlive: Boolean get() = process.isAlive

  override fun waitFor(timeout: Int, unit: TimeUnit): Boolean = process.waitFor(timeout.toLong(), unit)

  override fun waitFor(): Int = process.waitFor()

  override fun destroy(): Unit = process.destroyForcibly().let { }

  override suspend fun awaitExit(): Int = process.awaitExit()
}
