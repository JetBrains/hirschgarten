package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.TestStatus
import com.intellij.build.events.BuildEvent as IJBuildEvent
import com.intellij.build.events.BuildEvents
import com.intellij.build.events.MessageEvent
import org.jetbrains.bazel.label.Label

/**
 * Parses BEP TestSummary events into Build View messages.
 *
 * TestSummary provides aggregate information about test execution (e.g., how many attempts,
 * overall status). This complements TestResult which reports individual test run details.
 */
class TestSummaryParser : BuildEventParser {

  override fun parse(event: BuildEvent): IJBuildEvent? {
    if (!event.id.hasTestSummary()) return null
    if (!event.hasTestSummary()) return null

    val summary = event.testSummary
    val label = event.id.testSummary.label

    // Only report failures and warnings
    val overallStatus = summary.overallStatus
    when (overallStatus) {
      TestStatus.PASSED, TestStatus.NO_STATUS, TestStatus.UNRECOGNIZED -> return null
      else -> {} // Report failures
    }

    val parsedLabel = try {
      Label.parse(label)
    } catch (e: IllegalArgumentException) {
      null
    }

    val kind = when (overallStatus) {
      TestStatus.FLAKY -> MessageEvent.Kind.WARNING
      TestStatus.TIMEOUT, TestStatus.FAILED, TestStatus.INCOMPLETE -> MessageEvent.Kind.ERROR
      else -> MessageEvent.Kind.ERROR
    }

    val title = "Test Summary ${overallStatus.name}: $label"
    val description = buildString {
      appendLine(title)
      appendLine()
      appendLine("Total runs: ${summary.totalRunCount}")
      if (summary.hasFirstStartTime()) {
        val startMs = summary.firstStartTime.seconds * 1000 + summary.firstStartTime.nanos / 1_000_000
        appendLine("First start: ${java.time.Instant.ofEpochMilli(startMs)}")
      }
      if (summary.hasTotalRunDuration()) {
        val durationSec = summary.totalRunDuration.seconds
        appendLine("Total duration: ${durationSec}s")
      }

      // Report individual run results if multiple attempts
      if (summary.totalRunCount > 1) {
        appendLine()
        appendLine("Run attempts:")
        summary.passedList.forEach { appendLine("  - Passed: $it") }
        summary.failedList.forEach { appendLine("  - Failed: $it") }
      }
    }

    val issue = BazelBuildIssue(
      label = parsedLabel,
      title = title,
      description = description.trim(),
    )

    return BuildEvents.getInstance().message()
      .withMessage(title)
      .withDescription(description.trim())
      .withKind(kind)
      .build()
  }
}
