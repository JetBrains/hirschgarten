package org.jetbrains.bazel.bazelrunner

import org.jetbrains.bazel.bazelrunner.outputs.AsyncOutputProcessor
import org.jetbrains.bazel.bazelrunner.outputs.OutputProcessor
import org.jetbrains.bazel.bazelrunner.outputs.SpawnedProcess
import org.jetbrains.bazel.bazelrunner.outputs.SyncOutputProcessor
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.commons.Format
import org.jetbrains.bazel.commons.Stopwatch
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.logger.bazelLogger
import java.time.Duration
import java.util.concurrent.CompletableFuture

class BazelProcess internal constructor(
  val process: SpawnedProcess,
  private val logger: BspClientLogger? = null,
  private val serverPidFuture: CompletableFuture<Long>?,
  private val finishCallback: () -> Unit = {},
) {
  suspend fun waitAndGetResult(ensureAllOutputRead: Boolean = false): BazelProcessResult {
    return try {
      val stopwatch = Stopwatch.start()
      val outputProcessor: OutputProcessor =
        if (logger != null) {
          if (ensureAllOutputRead) {
            SyncOutputProcessor(process, logger::messageWithoutNewLine)
          } else {
            AsyncOutputProcessor(process, logger::messageWithoutNewLine)
          }
        } else {
          if (ensureAllOutputRead) {
            SyncOutputProcessor(process, LOGGER::info)
          } else {
            AsyncOutputProcessor(process, LOGGER::info)
          }
        }

      val exitCode = outputProcessor.waitForExit(serverPidFuture)
      val duration = stopwatch.stop()
      logCompletion(exitCode, duration)
      return BazelProcessResult(outputProcessor.stdoutCollector, outputProcessor.stderrCollector, BazelStatus.fromExitCode(exitCode))
    } finally {
      finishCallback()
    }
  }

  private fun logCompletion(exitCode: Int, duration: Duration) {
    logger?.message("\nCommand completed in ${Format.duration(duration)} (exit code $exitCode)")
  }

  companion object {
    private val LOGGER = bazelLogger<BazelProcess>()
  }
}
