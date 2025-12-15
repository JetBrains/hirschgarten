package org.jetbrains.bazel.sync_new.graph

import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.asClosingSequence
import org.jetbrains.bazel.sync_new.storage.put
import org.jetbrains.bazel.sync_new.storage.remove
import org.jetbrains.bazel.sync_new.storage.set
import kotlin.sequences.mapNotNull

// TODO: make hashed version of it
// TODO: make version od this with generic edge type
abstract class PersistentDirectedGraph<ID, V> : SimpleDirectedGraph<V> {
  protected abstract val id2Vertex: KVStore<ID, V>
  protected abstract val id2Successors: KVStore<ID, Set<ID>>
  protected abstract val id2Predecessors: KVStore<ID, Set<ID>>

  abstract fun getVertexId(vertex: V): ID
  fun getVertexById(id: ID): V? = id2Vertex[id]

  override val vertices: Sequence<V>
    get() = id2Vertex.values().asClosingSequence()

  override fun getSuccessors(vertex: V): Sequence<V> {
    val vertexId = getVertexId(vertex)
    val sequence = id2Successors[vertexId]?.asSequence() ?: emptySequence()
    return sequence.mapNotNull { id2Vertex[it] }
  }

  override fun getPredecessors(vertex: V): Sequence<V> {
    val vertexId = getVertexId(vertex)
    val sequence = id2Predecessors[vertexId]?.asSequence() ?: emptySequence()
    return sequence.mapNotNull { id2Vertex[it] }
  }

  override fun addVertex(vertex: V) {
    id2Vertex[getVertexId(vertex)] = vertex
  }

  override fun removeVertex(vertex: V) {
    val vertex = id2Vertex.remove(getVertexId(vertex)) ?: return
    getSuccessors(vertex).forEach { removeEdge(vertex, it) }
    getPredecessors(vertex).forEach { removeEdge(it, vertex) }
  }

  override fun addEdge(from: V, to: V) {
    val fromHash = getVertexId(from)
    val toHash = getVertexId(to)
    id2Successors.put(fromHash, toHash)
    id2Predecessors.put(toHash, fromHash)
  }

  override fun removeEdge(from: V, to: V) {
    val fromId = getVertexId(from)
    val toId = getVertexId(to)
    id2Successors.remove(fromId, toId)
    id2Predecessors.remove(toId, fromId)
  }

  override fun clear() {
    this.id2Vertex.clear()
    this.id2Successors.clear()
    this.id2Predecessors.clear()
  }
}
