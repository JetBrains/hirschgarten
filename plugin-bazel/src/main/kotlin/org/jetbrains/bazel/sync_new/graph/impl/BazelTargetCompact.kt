package org.jetbrains.bazel.sync_new.graph.impl

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetCompact

data class BazelTargetCompact(
  override val vertexId: ID,
  override val label: Label,
  override val isExecutable: Boolean = false
) : TargetCompact
