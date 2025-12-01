package org.jetbrains.bazel.sync_new.flow.universe

object SyncUniverseQuery {
  // TODO: use QueryBuilder dsl
  fun createUniverseQuery(patterns: Iterable<SyncUniverseTargetPattern>): String = buildString {
    val includes = patterns.filterIsInstance<SyncUniverseTargetPattern.Include>()
      .joinToString(separator = " ") { it.label.toString() }
    val excludes = patterns.filterIsInstance<SyncUniverseTargetPattern.Exclude>()
      .joinToString(separator = " ") { it.label.toString() }
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

  fun createSkyQueryUniverseScope(patterns: Iterable<SyncUniverseTargetPattern>): List<String> =
    patterns.map {
      when (it) {
        is SyncUniverseTargetPattern.Include -> it.label.toString()
        is SyncUniverseTargetPattern.Exclude -> "-${it.label}"
      }
    }
}
