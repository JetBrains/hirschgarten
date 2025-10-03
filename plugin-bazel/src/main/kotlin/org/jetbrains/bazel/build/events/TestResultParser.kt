package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.TestStatus
import com.intellij.build.events.BuildEvent as IJBuildEvent
import com.intellij.build.events.BuildEvents
import com.intellij.build.events.MessageEvent
import org.jetbrains.bazel.label.Label

/**
 * Parses BEP TestResult events into Build View messages.
 *
 * Reports test failures and provides navigable links to failing test targets.
 * Test results are shown with appropriate severity (error for failures, warning for flaky).
 */
class TestResultParser : BuildEventParser {

  override fun parse(event: BuildEvent): IJBuildEvent? {
    if (!event.id.hasTestResult()) return null
    if (!event.hasTestResult()) return null

    val testResult = event.testResult
    val label = event.id.testResult.label

    // Only report failures and warnings, not successful tests
    val status = testResult.status
    when (status) {
      TestStatus.PASSED, TestStatus.NO_STATUS, TestStatus.UNRECOGNIZED -> return null
      else -> {} // Report failures, flaky, timeouts, etc.
    }

    val parsedLabel = try {
      Label.parse(label)
    } catch (e: IllegalArgumentException) {
      null
    }

    val kind = when (status) {
      TestStatus.FLAKY -> MessageEvent.Kind.WARNING
      TestStatus.TIMEOUT -> MessageEvent.Kind.ERROR
      TestStatus.FAILED -> MessageEvent.Kind.ERROR
      TestStatus.INCOMPLETE -> MessageEvent.Kind.ERROR
      TestStatus.REMOTE_FAILURE -> MessageEvent.Kind.ERROR
      TestStatus.FAILED_TO_BUILD -> MessageEvent.Kind.ERROR
      TestStatus.TOOL_HALTED_BEFORE_TESTING -> MessageEvent.Kind.ERROR
      else -> MessageEvent.Kind.ERROR
    }

    val title = "Test ${status.name}: $label"
    val description = buildString {
      appendLine(title)
      if (testResult.hasTestAttemptDuration()) {
        val durationMs = testResult.testAttemptDuration.seconds * 1000 +
                         testResult.testAttemptDuration.nanos / 1_000_000
        appendLine("Duration: ${durationMs}ms")
      }
      if (testResult.statusDetails.isNotEmpty()) {
        appendLine()
        appendLine(testResult.statusDetails)
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
