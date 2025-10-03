package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.*
import com.intellij.build.events.BuildEvent as IJBuildEvent
import com.intellij.build.events.BuildEvents
import com.intellij.build.events.MessageEvent
import org.jetbrains.bazel.label.Label
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * Parses BEP ActionCompleted events into Build View issues.
 *
 * Ported from Google's Bazel plugin. This parser handles individual action failures
 * (e.g., compiler errors, test failures) reported through the Build Event Protocol.
 * It reads stderr output from file URIs and creates navigable issues that link back
 * to the failing target.
 */
class ActionCompletedParser : BuildEventParser {

  private fun getDescription(body: ActionExecuted, buildId: Any): String? {
    if (!body.hasStderr()) {
      if (body.hasFailureDetail()) {
        return body.failureDetail.message
      }
      return null
    }

    val uri = try {
      URI.create(body.stderr.uri)
    } catch (e: IllegalArgumentException) {
      return "Invalid output URI: ${body.stderr.uri}"
    }

    return try {
      Files.readString(Path.of(uri))
    } catch (e: IOException) {
      "Could not read output file: ${e.message}"
    }
  }

  override fun parse(event: BuildEvent): IJBuildEvent? {
    if (!event.id.hasActionCompleted()) return null
    val id = event.id.actionCompleted

    // Skip external project issues (they're outside our control)
    val isExternal = !id.label.startsWith("//")
    if (isExternal) return null

    if (!event.hasAction()) return null
    val body = event.action

    // Determine severity: if there's a failure detail, it's an error; otherwise it's a warning
    val isWarning = !body.hasFailureDetail()
    val kind = if (isWarning) MessageEvent.Kind.WARNING else MessageEvent.Kind.ERROR

    val description = getDescription(body, Any()) ?: return null
    val label = try {
      Label.parse(id.label)
    } catch (e: IllegalArgumentException) {
      null // Invalid label format
    }

    val title = if (isWarning) {
      "BUILD_WARNING: ${id.label}"
    } else {
      "BUILD_FAILURE: ${id.label}"
    }

    val issue = BazelBuildIssue(
      label = label,
      title = title,
      description = description,
    )

    // Convert to IntelliJ BuildEvent
    return BuildEvents.getInstance().message()
      .withMessage(title)
      .withDescription(description)
      .withKind(kind)
      .build()
  }
}
