package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.Aborted.AbortReason
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEventId
import com.intellij.build.events.BuildEvent as IJBuildEvent
import com.intellij.build.events.BuildEvents
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.bazel.label.Label

private val LOG = logger<AbortedParser>()

/**
 * Parses BEP Aborted events into Build View errors.
 *
 * Ported from Google's Bazel plugin. This parser handles targets that failed to build
 * due to missing dependencies, configuration errors, or other abort reasons reported
 * through the Build Event Protocol.
 */
class AbortedParser : BuildEventParser {

  private fun <T> reportMissingImplementation(id: BuildEventId): T? {
    LOG.error("missing implementation for BuildEventId: $id")
    return null
  }

  private fun getDescription(id: BuildEventId): String? {
    return when {
      id.hasUnconfiguredLabel() ->
        "Could not find label: ${id.unconfiguredLabel.label} - make sure a label or a file with that name exists"
      id.hasTargetConfigured() ->
        "Could not configure target: ${id.targetConfigured.label}"
      id.hasTargetCompleted() ->
        "Could not complete target: ${id.targetCompleted.label}"
      id.hasConfiguredLabel() -> null
      else -> reportMissingImplementation(id)
    }
  }

  private fun getChildDescription(id: BuildEventId): String? {
    return when {
      id.hasUnconfiguredLabel() ->
        "Depends on undefined label: ${id.unconfiguredLabel.label} - might not be a direct dependency"
      id.hasConfiguredLabel() -> null
      else -> reportMissingImplementation(id)
    }
  }

  private fun buildDescription(event: BuildEvent): String? {
    val builder = StringBuilder()

    if (event.aborted.description.isNotBlank()) {
      builder.append(event.aborted.description)
    } else {
      builder.append(getDescription(event.id) ?: return null)
    }
    builder.append("\n\n")

    for (child in event.childrenList) {
      val description = getChildDescription(child) ?: continue
      builder.append(description)
      builder.append("\n")
    }

    return builder.toString().trimEnd()
  }

  private fun getLabel(id: BuildEventId): String? {
    return when {
      id.hasTargetConfigured() -> id.targetConfigured.label
      id.hasTargetCompleted() -> id.targetCompleted.label
      id.hasUnconfiguredLabel() -> id.unconfiguredLabel.label
      id.hasConfiguredLabel() -> id.configuredLabel.label
      id.hasPattern() -> id.pattern.patternList.firstOrNull()
      id.hasUnstructuredCommandLine() -> null
      else -> reportMissingImplementation(id)
    }
  }

  override fun parse(event: BuildEvent): IJBuildEvent? {
    if (!event.hasAborted()) return null

    // Do not report skipped targets, this event is expected because sync is executed
    // with `--keep_going` or `--skip_incompatible_explicit_targets`
    if (event.aborted.reason == AbortReason.SKIPPED) return null

    val labelStr = getLabel(event.id) ?: return null
    val label = try {
      Label.parse(labelStr)
    } catch (e: IllegalArgumentException) {
      null // Invalid label format
    }

    val description = buildDescription(event) ?: return null
    val title = "${event.aborted.reason}: $labelStr"

    val issue = BazelBuildIssue(
      label = label,
      title = title,
      description = description,
    )

    // Convert to IntelliJ BuildEvent
    return BuildEvents.getInstance().message()
      .withMessage(title)
      .withDescription(description)
      .withKind(MessageEvent.Kind.ERROR)
      .build()
  }
}
