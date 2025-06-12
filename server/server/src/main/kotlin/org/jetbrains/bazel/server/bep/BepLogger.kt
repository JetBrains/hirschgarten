package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import org.jetbrains.bazel.commons.Format
import org.jetbrains.bazel.logger.BspClientLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

class BepLogger(private val bspClientLogger: BspClientLogger) {
  fun onProgress(progress: BuildEventStreamProtos.Progress) {
    val output = progress.stderr
    if (output.isNotBlank()) {
      logMessage(output)
      LOGGER.info(output)
    }
  }

  fun onBuildMetrics(metrics: BuildEventStreamProtos.BuildMetrics) {
    // TODO: https://youtrack.jetbrains.com/issue/BAZEL-621
    val duration = Duration.ofMillis(metrics.timingMetrics.wallTimeInMs)
    logMessage(String.format("Command completed in %s", Format.duration(duration)))
  }

  private fun logMessage(output: String) {
    bspClientLogger.message(output)
    val filteredOutput =
      output
        .split('\n')
        .dropLastWhile { it.isEmpty() }
        .filter { !it.startsWith(ADDITIONAL_MESSAGE_PREFIX) }
        .joinToString("\n")
    LOGGER.info(filteredOutput)
  }

  companion object {
    private val LOGGER: Logger =
      LoggerFactory.getLogger(
        BspClientLogger::class.java,
      )

    private const val ADDITIONAL_MESSAGE_PREFIX = "    "
  }
}
