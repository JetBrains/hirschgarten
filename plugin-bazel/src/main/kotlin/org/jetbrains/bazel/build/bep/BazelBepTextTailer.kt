package org.jetbrains.bazel.build.bep

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bazel.build.session.BazelBuildSession
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

/**
 * Minimal BEP JSONL tailer that watches a --build_event_text_file and extracts target events.
 *
 * We intentionally avoid depending on protobufs here; we only look for labels in common BEP ids:
 *   - id.targetConfigured.label
 *   - id.targetCompleted.label
 * and optionally detect success from a "completed":{"success":true} flag that may appear nearby.
 */
internal class BazelBepTextTailer(
  private val file: File,
  private val session: BazelBuildSession,
) : Runnable {

  private val log = logger<BazelBepTextTailer>()
  private val running = AtomicBoolean(false)
  private var thread: Thread? = null

  fun start() {
    if (!Registry.get("bazel.buildEvents.bep.enabled").asBoolean()) return
    if (running.getAndSet(true)) return
    thread = Thread(this, "BazelBepTextTailer").apply { isDaemon = true; start() }
  }

  fun stop() {
    running.set(false)
  }

  override fun run() {
    try {
      tailFile()
    }
    catch (t: Throwable) {
      log.warn("BEP tailer failed", t)
    }
  }

  private fun tailFile() {
    if (!file.exists()) return
    RandomAccessFile(file, "r").use { raf ->
      var pos = raf.length()
      while (running.get()) {
        val len = raf.length()
        if (len < pos) {
          // truncated
          pos = len
        }
        else if (len > pos) {
          raf.seek(pos)
          var line = raf.readLine()
          while (line != null) {
            handleLine(line)
            pos = raf.filePointer
            line = raf.readLine()
          }
        }
        Thread.sleep(100)
      }
    }
  }

  private fun handleLine(raw: String) {
    // The text file uses JSON per line. We use string matching for performance and laxity.
    val line = raw.trim()
    val label = extractLabel(line) ?: return

    when {
      line.contains("\"targetConfigured\"") -> session.onTargetConfigured(label)
      line.contains("\"targetCompleted\"") -> {
        val success = SUCCESS_TRUE.matcher(line).find()
        session.onTargetCompleted(label, success)
      }
    }
  }

  private fun extractLabel(line: String): String? {
    val m = LABEL.matcher(line)
    return if (m.find()) m.group(1) else null
  }

  companion object {
    private val LABEL: Pattern = Pattern.compile("\\\"label\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
    private val SUCCESS_TRUE: Pattern = Pattern.compile("\\\"success\\\"\\s*:\\s*true")
  }
}
