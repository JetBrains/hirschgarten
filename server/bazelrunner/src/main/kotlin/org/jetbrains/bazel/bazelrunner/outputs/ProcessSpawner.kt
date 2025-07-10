package org.jetbrains.bazel.bazelrunner.outputs

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.outputs.SpawnedProcess

/**
 * Interface for spawning and managing processes
 */
interface ProcessSpawner {
  suspend fun spawnDeferredProcess(
    command: List<String>,
    environment: Map<String, String>,
    redirectErrorStream: Boolean,
    workDirectory: String?,
  ): Deferred<SpawnedProcess>

  /**
   * Kill a process tree (the process and all its child processes)
   */
  fun killProcessTree(process: SpawnedProcess): Boolean

  /**
   * Kill a process
   */
  fun killProcess(process: SpawnedProcess)

  /**
   * Kill a process by its PID
   */
  fun killProcess(pid: Int)

  companion object {
    private lateinit var instance: ProcessSpawner

    fun getInstance(): ProcessSpawner =
      if (Companion::instance.isInitialized) instance else throw IllegalStateException("ProcessSpawner not initialized")

    fun provideProcessSpawner(spawner: ProcessSpawner) {
      instance = spawner
    }
  }
}

/**
 * Spawns a process and waits for it to complete
 */
fun ProcessSpawner.spawnProcess(
  command: List<String>,
  environment: Map<String, String>,
  redirectErrorStream: Boolean,
  workDirectory: String? = null,
): SpawnedProcess =
  runBlocking {
    spawnDeferredProcess(
      command = command,
      environment = environment,
      redirectErrorStream = redirectErrorStream,
      workDirectory = workDirectory,
    ).await()
  }
