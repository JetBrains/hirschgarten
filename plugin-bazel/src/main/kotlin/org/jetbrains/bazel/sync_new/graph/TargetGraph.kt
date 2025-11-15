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

interface TargetGraph<V : TargetVertex, E : TargetEdge> : ReferenceDirectedGraph<V, E> {
  fun getVertexByLabel(label: Label): V?
  fun getLabelByVertexId(id: ID): Label?

  fun getNextVertexId(): ID
  fun getNextEdgeId(): ID
}
