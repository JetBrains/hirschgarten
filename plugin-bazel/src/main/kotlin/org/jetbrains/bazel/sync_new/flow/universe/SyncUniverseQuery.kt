package org.jetbrains.bazel.sync_new.flow.universe

object SyncUniverseQuery {
  // TODO: use QueryBuilder dsl
  fun createUniverseQuery(pattern: Iterable<SyncUniverseTargetPattern>): String = buildString {
    val includes = pattern.filterIsInstance<SyncUniverseTargetPattern.Include>()
      .joinToString(separator = " ") { it.label.toString() }
    val excludes = pattern.filterIsInstance<SyncUniverseTargetPattern.Exclude>()
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
}
