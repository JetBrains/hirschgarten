package org.jetbrains.bazel.sync_new.flow.universe

import org.jetbrains.bazel.label.Label

sealed interface SyncUniverseTargetPattern {
  data class Include(val label: Label) : SyncUniverseTargetPattern
  data class Exclude(val label: Label) : SyncUniverseTargetPattern
}
