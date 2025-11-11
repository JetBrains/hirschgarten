package org.jetbrains.bazel.sync_new.graph

interface GraphEdge<V, E> {
  fun getFromVertex(graph: DirectedGraph<V, E>)
  fun getToVertex(graph: DirectedGraph<V, E>)
}

interface DirectedGraph<V, E> {
  val isAcyclic: Boolean
  val vertices: Sequence<V>
  val edges: Sequence<E>

  fun getOutgoingEdges(vertex: V): Sequence<E>
  fun getIncomingEdges(vertex: V): Sequence<E>

  fun getSuccessors(vertex: V): Sequence<V>
  fun getPredecessors(vertex: V): Sequence<V>

  fun addVertex(vertex: V)
  fun removeVertex(vertex: V)
  fun addEdge(edge: E)
  fun removeEdge(edge: E)
}

interface ReferenceDirectedGraph<ID, V, E> : DirectedGraph<V, E> {
  fun getVertexById(id: ID): V?
  fun getVertexId(vertex: V): ID?

  fun getOutgoingEdgesWithIds(id: ID): Sequence<E>
  fun getIncomingEdgesWithIds(id: ID): Sequence<E>

  fun getSuccessorsWithIds(id: ID): Sequence<ID>
  fun getPredecessorsWithIds(id: ID): Sequence<ID>
}
