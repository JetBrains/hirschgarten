package org.jetbrains.bazel.build.session

import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FileMessageEvent
import com.intellij.build.events.MessageEvent
import java.util.LinkedHashMap

/**
 * Best-effort de-duplication of build message events to avoid noisy duplicates
 * when tools echo the same diagnostic multiple times.
 */
internal class BazelBuildEventDeduplicator(private val maxSize: Int = 500) {

  private val lru = object : LinkedHashMap<String, Unit>(maxSize, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?): Boolean = size > maxSize
  }

  fun shouldAccept(event: BuildEvent): Boolean {
    val key = keyOf(event) ?: return true
    synchronized(lru) {
      if (lru.containsKey(key)) return false
      lru[key] = Unit
      return true
    }
  }

  private fun keyOf(event: BuildEvent): String? {
    if (event !is MessageEvent) return null
    val parent = event.parentId
    return when (event) {
      is FileMessageEvent -> buildString {
        append("F|")
        append(event.kind)
        append('|')
        append(event.message)
        append('|')
        val fp = event.filePosition
        append(fp.file?.path ?: "")
        append('|')
        append(fp.startLine)
        append('|')
        append(fp.startColumn)
        append('|')
        append(parent)
      }
      else -> buildString {
        append("M|")
        append(event.kind)
        append('|')
        append(event.message)
        append('|')
        append(parent)
      }
    }
  }
}
