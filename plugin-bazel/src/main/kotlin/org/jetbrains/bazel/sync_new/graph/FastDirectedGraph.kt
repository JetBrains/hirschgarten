package org.jetbrains.bazel.sync_new.graph

import it.unimi.dsi.fastutil.longs.LongList

typealias ID = Long
const val EMPTY_ID: ID = 0

interface FastDirectedGraph<V, E> {
  val vertices: Sequence<V>
  val edges: Sequence<E>

  fun getVertexById(id: ID): V?
  fun getEdgeById(id: ID): E?

  fun getSuccessors(id: ID): LongList
  fun getPredecessors(id: ID): LongList

  fun getOutgoingEdges(id: ID): LongList
  fun getIncomingEdges(id: ID): LongList

  fun getEdgeBetween(from: ID, to: ID): Long

  fun addVertex(vertex: V)
  fun addEdge(edge: E)
  fun removeVertexById(id: ID): V?
  fun removeEdgeById(id: ID): E?
}
