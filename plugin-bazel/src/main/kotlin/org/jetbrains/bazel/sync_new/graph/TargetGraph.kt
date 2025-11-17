package org.jetbrains.bazel.sync_new.graph

import it.unimi.dsi.fastutil.longs.LongList
import org.jetbrains.bazel.label.Label

typealias VertexId = Long
typealias EdgeId = Long
typealias VertexIdSequence = LongList
typealias EdgeIdSequence = LongList

interface TargetVertex {
  val id: ID
  val label: Label
}

interface TargetEdge {
  val id: ID
  val from: ID
  val to: ID
}

interface TargetCompact {
  val id: ID
  val label: Label
  val isExecutable: Boolean
}

interface TargetGraph<V : TargetVertex, E : TargetEdge, C : TargetCompact> : ReferenceDirectedGraph<V, E> {
  fun getVertexByLabel(label: Label): V?
  fun getLabelByVertexId(id: ID): Label?
  fun getTargetCompactById(id: ID): C?

  fun getNextVertexId(): ID
  fun getNextEdgeId(): ID
}
