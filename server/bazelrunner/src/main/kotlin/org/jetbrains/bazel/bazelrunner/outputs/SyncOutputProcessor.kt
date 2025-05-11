package org.jetbrains.bazel.bazelrunner.outputs

import org.jetbrains.bazel.bazelrunner.SpawnedProcess
import java.util.concurrent.TimeUnit

class SyncOutputProcessor(process: SpawnedProcess, vararg loggers: OutputHandler) : OutputProcessor(process, *loggers) {
  override fun isRunning(): Boolean = true

  override fun shutdown() {
    runningProcessors.forEach {
      it.get(1, TimeUnit.MINUTES) // Output handles should not be _that_ heavy
    }
    super.shutdown()
  }
}
