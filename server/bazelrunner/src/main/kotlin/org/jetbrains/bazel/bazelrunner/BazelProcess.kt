package org.jetbrains.bazel.bazelrunner

import com.intellij.util.io.outputStream
import org.jetbrains.bazel.bazelrunner.outputs.OutputProcessor
import org.jetbrains.bazel.commons.Format
import org.jetbrains.bazel.commons.Stopwatch
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bsp.protocol.BazelTaskLogger
import java.io.PrintWriter
import java.time.Instant

class BazelProcess internal constructor(
  private val process: Process,
  private val logger: BazelTaskLogger? = null,
  private val finishCallback: () -> Unit = {},
) {
  val pid: Long get() = process.pid()

  suspend fun waitAndGetResult(): BazelProcessResult {
    try {
      val stopwatch = Stopwatch.start()
      val outputProcessor =
        OutputProcessor(
          process,
          if (logger != null) logger::message else null,
        )

      val exitCode = outputProcessor.waitForExit(killProcessTreeOnCancel = BazelFeatureFlags.killClientTreeOnCancel)
      val duration = stopwatch.stop()
      logger?.info("Command completed in ${Format.duration(duration)} (exit code $exitCode)")

      writeBazelLog {
        appendLine("exit code: $exitCode")
        appendLine("stdout:\n${outputProcessor.stdoutCollector.raw().toString(Charsets.UTF_8)}")

        val stdErrLines = outputProcessor.stderrCollector.lines()
          .dropWhile { it.isBlank() }
          .dropLastWhile { it.isBlank() }
        if (stdErrLines.isNotEmpty()) {
          appendLine("stderr:")
          stdErrLines.forEach {
            appendLine(it)
          }
        }
      }

      return BazelProcessResult(outputProcessor.stdoutCollector, outputProcessor.stderrCollector, exitCode)
    } finally {
      finishCallback()
    }
  }

  fun destroy() {
    process.destroy()
  }

  internal fun writeBazelLog(body: PrintWriter.() -> Unit) {
    if (!BazelFeatureFlags.isLogEnabled)
      return

    BazelFeatureFlags.logPath.outputStream(append = true).use { out ->
      PrintWriter(out).use { writer ->
        try {
          writer.appendLine("\n${Instant.now()}")
          body(writer)
        } catch (ex: Throwable) {
          ex.printStackTrace(writer)
        }
      }
    }
  }
}
