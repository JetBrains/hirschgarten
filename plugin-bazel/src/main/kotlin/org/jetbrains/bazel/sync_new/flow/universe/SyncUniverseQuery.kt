package org.jetbrains.bazel.sync_new.flow.universe

import org.jetbrains.bazel.label.Label

object SyncUniverseQuery {
  // TODO: use QueryBuilder dsl
  fun createUniverseQuery(patterns: Iterable<SyncUniverseFilteredCondition<Label>>): String = buildString {
    val includes = patterns.filterIsInstance<SyncUniverseFilteredCondition.Include<Label>>()
      .joinToString(separator = " ") { it.element.toString() }
    val excludes = patterns.filterIsInstance<SyncUniverseFilteredCondition.Exclude<Label>>()
      .joinToString(separator = " ") { it.element.toString() }
    if (includes.isEmpty()) {
      append("set(//...)")
    } else {
      append("set($includes)")
    }
    if (excludes.isNotEmpty()) {
      append(" - set($excludes)")
    }
  }

  fun createUniverseQuery(state: SyncUniverseState): String = createUniverseQuery(state.importState.patterns)

  fun createSkyQueryUniverseScope(patterns: Iterable<SyncUniverseFilteredCondition<Label>>): List<String> =
    patterns.map {
      when (it) {
        is SyncUniverseFilteredCondition.Include<Label> -> it.element.toString()
        is SyncUniverseFilteredCondition.Exclude<Label> -> "-${it.element}"
      }
    }
}
