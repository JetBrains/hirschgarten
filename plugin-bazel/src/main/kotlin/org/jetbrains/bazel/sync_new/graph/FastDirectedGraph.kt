package org.jetbrains.bazel.sync_new.graph

import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntSet

typealias ID = Int
const val EMPTY_ID: ID = 0

interface FastDirectedGraph<V, E> {
  val vertices: Sequence<V>
  val edges: Sequence<E>

  fun getVertexById(id: ID): V?
  fun getEdgeById(id: ID): E?

  fun getSuccessors(id: ID): IntList
  fun getPredecessors(id: ID): IntList

  fun getOutgoingEdges(id: ID): IntList
  fun getIncomingEdges(id: ID): IntList

  fun getEdgeBetween(from: ID, to: ID): ID

  fun addVertex(vertex: V)
  fun addEdge(edge: E)
  fun removeVertexById(id: ID): V?
  fun removeEdgeById(id: ID): E?

  fun getAllVertexIds(): IntSet
}
