package org.jetbrains.bazel.sync_new.graph.impl

import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetEdge

data class BazelTargetEdge(
  override val edgeId: ID,
  override val from: ID,
  override val to: ID
) : TargetEdge
