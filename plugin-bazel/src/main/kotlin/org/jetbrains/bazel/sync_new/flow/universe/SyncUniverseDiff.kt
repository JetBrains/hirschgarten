package org.jetbrains.bazel.sync_new.flow.universe

import org.jetbrains.bazel.label.Label

class SyncUniverseDiff(
  val added: Set<Label> = emptySet(),
  val removed: Set<Label> = emptySet(),

  // used when projectview import options are changed,
  // then every target in the universe will appear here
  val changed: Set<Label> = emptySet(),
) {
  val hasChanged: Boolean = added.isNotEmpty() || removed.isNotEmpty() || changed.isNotEmpty()
}
