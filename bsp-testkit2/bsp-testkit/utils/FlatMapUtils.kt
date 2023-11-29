package org.jetbrains.bsp.testkit

import java.util.AbstractMap

object FlatMapUtils {
  fun flatten(map: Map<String, Any>): Map<String, Any> {
    return map.entries.flatMap { flatten(it) }.associate { "/${it.key}" to it.value }
  }

  private fun flatten(entry: Map.Entry<String, Any>): List<Map.Entry<String, Any>> {
    return when (val value = entry.value) {
      is Map<*, *> -> value.entries.flatMap { e ->
        flatten(AbstractMap.SimpleEntry("${entry.key}/${e.key}", e.value!!))
      }

      is List<*> -> value.indices.map { i ->
        AbstractMap.SimpleEntry("${entry.key}/$i", value[i]!!)
      }.flatMap { flatten(it) }

      else -> listOf(entry)
    }
  }
}
