package org.jetbrains.bazel.bazelrunner.outputs

import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Interface representing a process that has been spawned
 */
interface SpawnedProcess {
  /**
   * The process ID
   */
  val pid: Long

  /**
   * Gets the standard output stream from the process
   */
  val inputStream: InputStream

  /**
   * Gets the standard error stream from the process
   */
  val errorStream: InputStream

  /**
   * Checks if the process is still alive
   */
  val isAlive: Boolean

  fun waitFor(timeout: Int, unit: TimeUnit): Boolean

  fun waitFor(): Int

  /**
   * Attempts to terminate the process
   */
  fun destroy()

  suspend fun awaitExit(): Int
}
