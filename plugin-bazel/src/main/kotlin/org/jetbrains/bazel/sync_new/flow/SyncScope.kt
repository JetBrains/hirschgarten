package org.jetbrains.bazel.sync_new.flow

import org.jetbrains.bazel.label.Label
import java.nio.file.Path

sealed interface SyncScope {
  data class Full(val build: Boolean = false) : SyncScope
  object Incremental : SyncScope
  data class Partial(val targets: List<PartialTarget>) : SyncScope

  val isFullSync: Boolean
    get() = this is Full
}

sealed interface PartialTarget {
  data class ByLabel(val label: Label): PartialTarget
  data class ByFile(val file: Path): PartialTarget
}
