package org.jetbrains.bazel.sync_new.graph

interface SimpleDirectedGraph<V> {
  val vertices: Sequence<V>

  fun getSuccessors(vertex: V): Sequence<V>
  fun getPredecessors(vertex: V): Sequence<V>

  fun addVertex(vertex: V)
  fun removeVertex(vertex: V)
  fun addEdge(from: V, to: V)
  fun removeEdge(from: V, to: V)

  fun clear();
}
