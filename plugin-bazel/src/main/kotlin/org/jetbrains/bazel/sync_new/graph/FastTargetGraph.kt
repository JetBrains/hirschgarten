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

interface FastTargetGraph<V : TargetVertex, E : TargetEdge, C : TargetCompact> : FastDirectedGraph<V, E> {
  fun getVertexByLabel(label: Label): V?
  fun getVertexIdByLabel(label: Label): ID?
  fun getLabelByVertexId(id: ID): Label?
  fun getVertexCompactById(id: ID): C?

  fun getNextVertexId(): ID
  fun getNextEdgeId(): ID

  fun clear()
}
