package org.jetbrains.bazel.bazelrunner.outputs

import java.util.concurrent.TimeUnit

class SyncOutputProcessor(
  process: SpawnedProcess,
  vararg loggers: OutputHandler,
  shouldReadStdout: Boolean = true,
  shouldReadStderr: Boolean = true,
) : OutputProcessor(process, *loggers, shouldReadStdout = shouldReadStdout, shouldReadStderr = shouldReadStderr) {
  override fun isRunning(): Boolean = true

  override fun shutdown() {
    runningProcessors.forEach {
      it.get(1, TimeUnit.MINUTES) // Output handles should not be _that_ heavy
    }
    super.shutdown()
  }
}
