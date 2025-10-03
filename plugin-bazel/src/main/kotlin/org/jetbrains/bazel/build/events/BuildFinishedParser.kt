package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.intellij.build.events.BuildEvent as IJBuildEvent
import com.intellij.build.events.BuildEvents
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.diagnostic.logger

private val LOG = logger<BuildFinishedParser>()

/**
 * Parses BEP BuildFinished events to provide build completion summary.
 *
 * This event marks the end of the build and includes the final exit code and any
 * anomaly reports (e.g., crashes, resource exhaustion). We can use this to provide
 * a final build summary message in the Build View.
 */
class BuildFinishedParser : BuildEventParser {

  override fun parse(event: BuildEvent): IJBuildEvent? {
    if (!event.id.hasBuildFinished()) return null
    if (!event.hasFinished()) return null

    val finished = event.finished
    val exitCode = finished.exitCode.code

    // Only report non-zero exit codes or anomaly reports
    val hasAnomalyReport = finished.hasAnomalyReport()

    if (exitCode == 0 && !hasAnomalyReport) {
      // Build succeeded with no anomalies - no need to report
      return null
    }

    val kind = if (exitCode == 0) MessageEvent.Kind.WARNING else MessageEvent.Kind.ERROR

    val title = if (exitCode == 0) {
      "Build completed with warnings"
    } else {
      "Build failed with exit code $exitCode"
    }

    val description = buildString {
      appendLine(title)

      if (finished.hasFinishTime()) {
        val finishMs = finished.finishTime.seconds * 1000 + finished.finishTime.nanos / 1_000_000
        appendLine("Finished at: ${java.time.Instant.ofEpochMilli(finishMs)}")
      }

      if (hasAnomalyReport) {
        appendLine()
        appendLine("Anomaly Report:")
        appendLine(finished.anomalyReport)
      }

      if (finished.exitCode.name.isNotEmpty()) {
        appendLine()
        appendLine("Exit code: ${finished.exitCode.name}")
      }
    }

    LOG.info("Build finished with exit code $exitCode")

    return BuildEvents.getInstance().message()
      .withMessage(title)
      .withDescription(description.trim())
      .withKind(kind)
      .build()
  }
}
