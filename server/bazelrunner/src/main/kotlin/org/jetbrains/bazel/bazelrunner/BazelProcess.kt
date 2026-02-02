package org.jetbrains.bazel.bazelrunner

import org.jetbrains.bazel.bazelrunner.outputs.OutputProcessor
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.Format
import org.jetbrains.bazel.commons.Stopwatch
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.logger.BspClientLogger
import java.time.Duration

class BazelProcess internal constructor(
  private val process: Process,
  private val logger: BspClientLogger? = null,
  private val finishCallback: () -> Unit = {},
) {
  val pid: Long get() = process.pid()

  suspend fun waitAndGetResult(): BazelProcessResult {
    try {
      val stopwatch = Stopwatch.start()
      val outputProcessor =
        OutputProcessor(
          process,
          if (logger != null) logger::messageWithoutNewLine else null,
        )

      val exitCode = outputProcessor.waitForExit(killProcessTreeOnCancel = BazelFeatureFlags.killServerOnCancel)
      val duration = stopwatch.stop()
      logCompletion(exitCode, duration)
      return BazelProcessResult(outputProcessor.stdoutCollector, outputProcessor.stderrCollector, BazelStatus.fromExitCode(exitCode))
    } finally {
      finishCallback()
    }
  }

  fun destroy() {
    process.destroy()
  }

  private fun logCompletion(exitCode: Int, duration: Duration) {
    logger?.message("\nCommand completed in ${Format.duration(duration)} (exit code $exitCode)")
  }
}
