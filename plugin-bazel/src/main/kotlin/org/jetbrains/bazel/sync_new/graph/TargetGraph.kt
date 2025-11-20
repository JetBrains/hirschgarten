package org.jetbrains.bazel.sync_new.graph

import org.jetbrains.bazel.label.Label

interface TargetVertex {
  val vertexId: ID
  val label: Label
}

interface TargetEdge {
  val edgeId: ID
  val from: ID
  val to: ID
}

interface TargetCompact {
  val vertexId: ID
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
