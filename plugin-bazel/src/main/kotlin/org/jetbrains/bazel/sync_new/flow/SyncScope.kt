package org.jetbrains.bazel.sync_new.flow

import org.jetbrains.bazel.label.Label

sealed interface SyncScope {
  data class Full(val build: Boolean = false) : SyncScope
  object Incremental : SyncScope
  data class Partial(val targets: List<Label>) : SyncScope
}
