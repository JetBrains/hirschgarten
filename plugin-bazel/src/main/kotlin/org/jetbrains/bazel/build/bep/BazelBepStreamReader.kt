package org.jetbrains.bazel.build.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bazel.build.events.BuildEventParser
import org.jetbrains.bazel.build.session.BazelBuildSession
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private val LOG = logger<BazelBepStreamReader>()

/**
 * Reads and parses Build Event Protocol (BEP) events from a binary protobuf file.
 *
 * Unlike the previous JSON-based approach, this uses Bazel's official binary protobuf format
 * (--build_event_binary_file) which is:
 * - More efficient (smaller files, faster parsing)
 * - More robust (no regex parsing, proper structure)
 * - Better aligned with Google plugin's approach
 * - Provides richer event data through proper protobuf deserialization
 *
 * The file contains a stream of delimited BuildEvent protobuf messages.
 * We read them continuously as they're written by Bazel.
 */
internal class BazelBepStreamReader(
  private val file: File,
  private val session: BazelBuildSession,
) : Runnable {

  private val running = AtomicBoolean(false)
  private var thread: Thread? = null

  fun start() {
    if (!Registry.get("bazel.buildEvents.bep.enabled").asBoolean()) return
    if (running.getAndSet(true)) return
    thread = Thread(this, "BazelBepStreamReader").apply {
      isDaemon = true
      start()
    }
  }

  fun stop() {
    running.set(false)
    thread?.interrupt()
  }

  override fun run() {
    try {
      readStream()
    } catch (t: Throwable) {
      if (t !is InterruptedException) {
        LOG.warn("BEP stream reader failed", t)
      }
    }
  }

  private fun readStream() {
    // Wait for file to be created by Bazel
    while (running.get() && !file.exists()) {
      Thread.sleep(100)
    }

    if (!running.get()) return

    BufferedInputStream(FileInputStream(file)).use { input ->
      while (running.get()) {
        try {
          // Read next BuildEvent from the delimited protobuf stream
          val event = BuildEvent.parseDelimitedFrom(input) ?: break
          handleEvent(event)
        } catch (e: IOException) {
          if (running.get()) {
            LOG.warn("Error reading BEP event", e)
          }
          break
        }
      }
    }
  }

  private fun handleEvent(event: BuildEvent) {
    // First, update target grouping if this is a target event
    when {
      event.id.hasTargetConfigured() -> {
        val label = event.id.targetConfigured.label
        session.onTargetConfigured(label)
      }
      event.id.hasTargetCompleted() -> {
        val label = event.id.targetCompleted.label
        val success = event.hasCompleted() && event.completed.success
        session.onTargetCompleted(label, success)
      }
    }

    // Then, try to parse into a Build View event
    val buildEvent = BuildEventParser.parse(event)
    if (buildEvent != null) {
      session.accept(buildEvent)
    }
  }
}
