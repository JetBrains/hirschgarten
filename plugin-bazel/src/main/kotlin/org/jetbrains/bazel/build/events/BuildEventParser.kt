package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.intellij.build.events.BuildEvent as IJBuildEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName

private val LOG = logger<BuildEventParser>()

/**
 * Extension point for parsing Build Event Protocol (BEP) events into IntelliJ BuildEvents.
 *
 * Ported from Google's Bazel plugin to provide extensible BEP event handling.
 * Implementations can parse specific BEP event types (e.g., ActionCompleted, Aborted)
 * and convert them into appropriate IntelliJ BuildEvents for the Build View.
 *
 * To register a parser, add to plugin.xml:
 * ```xml
 * <bazel.buildEventParser implementation="your.parser.Class"/>
 * ```
 */
interface BuildEventParser {
  companion object {
    val EP_NAME: ExtensionPointName<BuildEventParser> =
      ExtensionPointName.create("org.jetbrains.bazel.buildEventParser")

    /**
     * Parse a BEP event using all registered parsers.
     * Returns the first non-null result, or null if no parser handled the event.
     */
    fun parse(event: BuildEvent): IJBuildEvent? {
      return EP_NAME.extensionList.firstNotNullOfOrNull { parser ->
        try {
          parser.parse(event)
        } catch (t: Throwable) {
          LOG.error("Parser ${parser.javaClass.simpleName} failed to parse event", t)
          null
        }
      }
    }
  }

  /**
   * Parse a BEP event into an IntelliJ BuildEvent.
   * Return null if this parser doesn't handle this event type.
   */
  fun parse(event: BuildEvent): IJBuildEvent?
}
