package org.jetbrains.bazel.sync_new.graph.impl

import com.dynatrace.hash4j.hashing.HashValue128
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.longs.LongLists
import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofLong
import org.jetbrains.bazel.sync_new.graph.EMPTY_ID
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.FastTargetGraph
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.createSortedKVStore
import org.jetbrains.bazel.sync_new.storage.getValue
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.hash128Comparator
import org.jetbrains.bazel.sync_new.storage.hash.putResolvedLabel
import org.jetbrains.bazel.sync_new.storage.set

class BazelFastTargetGraph(val storage: StorageContext) : FastTargetGraph<BazelTargetVertex, BazelTargetEdge, BazelTargetCompact> {
  private val id2Vertex =
    storage.createSortedKVStore<ID, BazelTargetVertex>("bazel.target.graph.id2vertex", StorageHints.USE_PAGED_STORE)
      .withKeyComparator { compareBy { it } }
      .withKeyCodec { ofLong() }
      .withValueCodec { ofKryo() }
      .build()

  private val id2Edge =
    storage.createSortedKVStore<ID, BazelTargetEdge>("bazel.target.graph.id2edge", StorageHints.USE_PAGED_STORE)
      .withKeyComparator { compareBy { it } }
      .withKeyCodec { ofLong() }
      .withValueCodec { ofKryo() }
      .build()

  private val id2Compact =
    storage.createSortedKVStore<ID, BazelTargetCompact>("bazel.target.graph.id2label", StorageHints.USE_PAGED_STORE)
      .withKeyComparator { compareBy { it } }
      .withKeyCodec { ofLong() }
      .withValueCodec { ofKryo() }
      .build()

  private val labelHash2VertexId by storage.createFlatStore<Object2LongMap<HashValue128>>(
    "bazel.target.graph.labelHash2vertexId",
    StorageHints.USE_IN_MEMORY,
  )
    .withCreator { Object2LongOpenHashMap() }
    .withCodec { ofKryo() }
    .build()

  private val edgeLink2EdgeId = storage.createSortedKVStore<HashValue128, Long>(
    "bazel.target.graph.edgeLink2EdgeId",
    StorageHints.USE_IN_MEMORY,
  )
    .withKeyComparator { hash128Comparator() }
    .withKeyCodec { ofHash128() }
    .withValueCodec { ofLong() }
    .build()

  private val id2Successors by storage.createFlatStore<Long2ObjectMap<LongList>>(
    "bazel.target.graph.id2Successors",
    StorageHints.USE_IN_MEMORY,
  )
    .withCreator { Long2ObjectOpenHashMap() }
    .withCodec { ofKryo() }
    .build()

  private val id2Predecessors by storage.createFlatStore<Long2ObjectMap<LongList>>(
    "bazel.target.graph.id2Predecessors",
    StorageHints.USE_IN_MEMORY,
  )
    .withCreator { Long2ObjectOpenHashMap() }
    .withCodec { ofKryo() }
    .build()

  override val vertices: Sequence<BazelTargetVertex>
    get() = id2Vertex.values()
  override val edges: Sequence<BazelTargetEdge>
    get() = id2Edge.values()

  override fun getVertexById(id: ID): BazelTargetVertex? = id2Vertex.get(id)

  override fun getVertexByLabel(label: Label): BazelTargetVertex? {
    val vertexId = getVertexIdByLabel(label)
    if (vertexId == EMPTY_ID) {
      return null
    }
    return getVertexById(vertexId)
  }

  override fun getVertexIdByLabel(label: Label): ID {
    val hash = hash { putResolvedLabel(label as ResolvedLabel) }
    return labelHash2VertexId.getLong(hash)
  }

  override fun getLabelByVertexId(id: ID): Label? = id2Compact.get(id)?.label
  override fun getTargetCompactById(id: ID): BazelTargetCompact? = id2Compact.get(id)

  override fun getEdgeById(id: ID): BazelTargetEdge? = id2Edge.get(id)

  override fun getSuccessors(id: ID): LongList = id2Successors.get(id) ?: LongLists.EMPTY_LIST

  override fun getPredecessors(id: ID): LongList = id2Predecessors.get(id) ?: LongLists.EMPTY_LIST

  override fun getOutgoingEdges(id: ID): LongList {
    val edges = LongArrayList()
    getSuccessors(id)
      .map { getEdgeBetween(id, it) }
      .filter { it != EMPTY_ID }
      .forEach { edges.add(it) }
    return edges
  }

  override fun getIncomingEdges(id: ID): LongList {
    val edges = LongArrayList()
    getPredecessors(id)
      .map { getEdgeBetween(it, id) }
      .filter { it != EMPTY_ID }
      .forEach { edges.add(it) }
    return edges
  }

  override fun getEdgeBetween(
    from: ID,
    to: ID,
  ): Long {
    return edgeLink2EdgeId[HashValue128(from, to)] ?: EMPTY_ID
  }

  override fun addVertex(vertex: BazelTargetVertex) {
    id2Vertex.put(vertex.vertexId, vertex)
    id2Compact.put(vertex.vertexId, vertex.toTargetCompact())

    val labelHash = hash { putResolvedLabel(vertex.label as ResolvedLabel) }
    labelHash2VertexId.put(labelHash, vertex.vertexId)
  }

  override fun addEdge(edge: BazelTargetEdge) {
    id2Edge.put(edge.edgeId, edge)
    id2Successors.computeIfAbsent(edge.from) { LongArrayList() }.add(edge.to)
    id2Predecessors.computeIfAbsent(edge.to) { LongArrayList() }.add(edge.from)

    edgeLink2EdgeId[HashValue128(edge.from, edge.to)] = edge.edgeId
  }

  override fun removeVertexById(
    id: ID,
  ): BazelTargetVertex? {
    val vertex = id2Vertex.remove(key = id, useReturn = true) ?: return null
    id2Compact.remove(id)

    val labelHash = hash { putResolvedLabel(vertex.label as ResolvedLabel) }
    labelHash2VertexId.removeLong(labelHash)

    val successorIds = id2Successors.remove(vertex.vertexId)
    if (successorIds != null) {
      for (n in successorIds.indices) {
        val successor = successorIds.getLong(n)
        id2Predecessors.remove(successor, vertex.vertexId)
        val link = removeLinksBetween(vertex.vertexId, successor)
        if (link != EMPTY_ID) {
          id2Edge.remove(link)
        }
      }
    }

    val predecessorIds = id2Predecessors.remove(vertex.vertexId)
    if (predecessorIds != null) {
      for (n in predecessorIds.indices) {
        val predecessor = predecessorIds.getLong(n)
        id2Successors.remove(predecessor, vertex.vertexId)
        val link = removeLinksBetween(predecessor, vertex.vertexId)
        if (link != EMPTY_ID) {
          id2Edge.remove(link)
        }
      }
    }

    return vertex
  }

  override fun removeEdgeById(
    id: ID,
  ): BazelTargetEdge? {
    val edge = id2Edge.remove(key = id, useReturn = true) ?: return null
    id2Successors.remove(edge.from, edge.to)
    id2Predecessors.remove(edge.to, edge.from)

    edgeLink2EdgeId.remove(HashValue128(edge.from, edge.to))
    return edge
  }

  override fun getNextVertexId(): ID = getUsableId { id2Vertex.getHighestKey() }

  override fun getNextEdgeId(): ID = getUsableId { id2Edge.getHighestKey() }

  override fun clear() {
    id2Vertex.clear()
    id2Edge.clear()
    id2Compact.clear()
    labelHash2VertexId.clear()
    edgeLink2EdgeId.clear()
    id2Successors.clear()
    id2Predecessors.clear()
  }

  // id `0` is reserved as an invalid state or absence of value
  private fun getUsableId(getter: () -> Long?): Long {
    val id = getter() ?: return 1
    return maxOf(id + 1, 1)
  }

  private fun removeLinksBetween(form: ID, to: ID): Long {
    return edgeLink2EdgeId.remove(HashValue128(form, to)) ?: EMPTY_ID
  }

  private fun BazelTargetVertex.toTargetCompact(): BazelTargetCompact {
    return BazelTargetCompact(
      label = label,
      vertexId = vertexId,
      isExecutable = BazelTargetTag.EXECUTABLE in genericData.tags,
    )
  }

}
