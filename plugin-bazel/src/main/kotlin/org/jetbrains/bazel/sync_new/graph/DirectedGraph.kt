package org.jetbrains.bazel.sync_new.graph

interface GraphEdge<V> {
  val from: V
  val to: V
}

interface DirectedGraph<V, E : GraphEdge<V>> {
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

interface ReferenceDirectedGraph<ID, V, E> : DirectedGraph<V, E>
  where E : ReferenceGraphEdge<ID, V> {
  val verticesIds: Sequence<ID>
  val edgesIds: Sequence<E>

  fun getVertexById(id: ID): V?
  fun getVertexId(vertex: V): ID?

  fun getEdgeById(id: ID): E?
  fun getEdgeId(edge: E): ID?

  fun getOutgoingEdgesIds(id: ID): Sequence<ID>
  fun getIncomingEdgesIds(id: ID): Sequence<ID>

  fun getSuccessorsWithIds(id: ID): Sequence<ID>
  fun getPredecessorsWithIds(id: ID): Sequence<ID>

  fun removeVertexById(id: ID)
  fun removeEdgeById(id: ID)
}

interface ReferenceGraphEdge<ID, V> : GraphEdge<V> {
  val fromId: ID
  val toId: ID
}

abstract class AbstractReferenceDirectedGraph<ID, V, E> : ReferenceDirectedGraph<ID, V, E>
  where E : ReferenceGraphEdge<ID, V> {
  override val isAcyclic: Boolean = true

  override fun getOutgoingEdges(vertex: V): Sequence<E> =
    getVertexId(vertex)?.let {
      getOutgoingEdgesIds(it).mapNotNull { id -> getEdgeById(id) }
    } ?: emptySequence()

  override fun getIncomingEdges(vertex: V): Sequence<E> =
    getVertexId(vertex)?.let {
      getIncomingEdgesIds(it).mapNotNull { id -> getEdgeById(id) }
    } ?: emptySequence()

  override fun getSuccessors(vertex: V): Sequence<V> = getVertexId(vertex)?.let { id ->
    getSuccessorsWithIds(id)
      .mapNotNull { getVertexById(it) }
  } ?: emptySequence()

  override fun getPredecessors(vertex: V): Sequence<V> = getVertexId(vertex)?.let { id ->
    getPredecessorsWithIds(id)
      .mapNotNull { getVertexById(it) }
  } ?: emptySequence()

  override fun removeEdge(edge: E) {
    getEdgeId(edge)?.let { removeEdgeById(it) }
  }

  override fun removeVertex(vertex: V) {
    getVertexId(vertex)?.let { removeVertexById(it) }
  }

}
