package org.jetbrains.bazel.bazelrunner.outputs

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class AsyncOutputProcessor(
  process: SpawnedProcess,
  vararg loggers: OutputHandler,
  shouldReadStdout: Boolean = true,
  shouldReadStderr: Boolean = true,
) : OutputProcessor(process, *loggers, shouldReadStdout = shouldReadStdout, shouldReadStderr = shouldReadStderr) {
  private val isRunning = AtomicBoolean(true)

  override fun isRunning(): Boolean = isRunning.get()

  override fun shutdown() {
    isRunning.set(false)
    runningProcessors.forEach {
      try {
        it.get(500, TimeUnit.MILLISECONDS) // Output handles should not be _that_ heavy
      } catch (_: TimeoutException) {
        // ignore it
      }
    }
    super.shutdown()
  }
}
