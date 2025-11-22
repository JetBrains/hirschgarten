package org.jetbrains.bazel.sync_new.flow.diff

import org.jetbrains.bazel.label.Label

sealed interface TargetPattern {
  data class Include(val label: Label) : TargetPattern
  data class Exclude(val label: Label) : TargetPattern
}
