package org.jetbrains.bazel.sync_new.graph.impl

import com.dynatrace.hash4j.hashing.HashValue128
import com.jetbrains.rd.util.keySet
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntLists
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.sync_new.codec.Int2ObjectOpenHashMapCodec
import org.jetbrains.bazel.sync_new.codec.IntArrayListCodec
import org.jetbrains.bazel.sync_new.codec.Object2IntOpenHashMapCodec
import org.jetbrains.bazel.sync_new.codec.hash128Codec
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.codec.ofInt
import org.jetbrains.bazel.sync_new.codec.ofLong
import org.jetbrains.bazel.sync_new.codec.versionedCodecOf
import org.jetbrains.bazel.sync_new.graph.EMPTY_ID
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.FastTargetGraph
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.getValue
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.putResolvedLabel
import org.jetbrains.bazel.sync_new.storage.set

class BazelFastTargetGraph(val storage: StorageContext) : FastTargetGraph<BazelTargetVertex, BazelTargetEdge, BazelTargetCompact> {
  private val metadata = storage.createFlatStore<BazelGraphMetadata>("bazel.target.graph.metadata", StorageHints.USE_IN_MEMORY)
    .withCreator { BazelGraphMetadata() }
    .withCodec { ofKryo() }
    .build()

  private val id2Vertex =
    storage.createKVStore<ID, BazelTargetVertex>("bazel.target.graph.id2vertex", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofInt() }
      .withValueCodec { ofKryo() }
      .build()

  private val id2Edge =
    storage.createKVStore<ID, BazelTargetEdge>("bazel.target.graph.id2edge", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofInt() }
      .withValueCodec { ofKryo() }
      .build()

  private val id2Compact =
    storage.createKVStore<ID, BazelTargetCompact>("bazel.target.graph.id2label", StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofInt() }
      .withValueCodec { ofKryo() }
      .build()

  private val labelHash2VertexId by storage.createFlatStore<Object2IntOpenHashMap<HashValue128>>(
    "bazel.target.graph.labelHash2vertexId",
    StorageHints.USE_IN_MEMORY,
  )
    .withCreator { Object2IntOpenHashMap() }
    .withCodec {
      versionedCodecOf(
        version = 1,
        encode = { ctx, buffer, value ->
          Object2IntOpenHashMapCodec.encode(ctx, buffer, value) { buffer, value -> hash128Codec.encode(ctx, buffer, value) }
        },
        decode = { ctx, buffer, version ->
          check(version == 1) { "unsupported version" }
          Object2IntOpenHashMapCodec.decode(ctx, buffer) { buffer -> hash128Codec.decode(ctx, buffer) }
        },
      )
    }
    .build()

  private val edgeLink2EdgeId = storage.createKVStore<Long, Int>(
    "bazel.target.graph.edgeLink2EdgeId",
    StorageHints.USE_PAGED_STORE,
  )
    .withKeyCodec { ofLong() }
    .withValueCodec { ofInt() }
    .build()

  private val id2Successors by storage.createFlatStore<Int2ObjectOpenHashMap<IntArrayList>>(
    "bazel.target.graph.id2Successors",
    StorageHints.USE_IN_MEMORY,
  )
    .withCreator { Int2ObjectOpenHashMap() }
    .withCodec {
      versionedCodecOf(
        version = 1,
        encode = { ctx, buffer, value ->
          Int2ObjectOpenHashMapCodec.encode(ctx, buffer, value) { buffer, value -> IntArrayListCodec.encode(ctx, buffer, value) }
        },
        decode = { ctx, buffer, version ->
          check(version == 1) { "unsupported version" }
          Int2ObjectOpenHashMapCodec.decode(ctx, buffer) { buffer -> IntArrayListCodec.decode(ctx, buffer) }
        },
      )
    }
    .build()

  private val id2Predecessors by storage.createFlatStore<Int2ObjectOpenHashMap<IntArrayList>>(
    "bazel.target.graph.id2Predecessors",
    StorageHints.USE_IN_MEMORY,
  )
    .withCreator { Int2ObjectOpenHashMap() }
    .withCodec {
      versionedCodecOf(
        version = 1,
        encode = { ctx, buffer, value ->
          Int2ObjectOpenHashMapCodec.encode(ctx, buffer, value) { buffer, value -> IntArrayListCodec.encode(ctx, buffer, value) }
        },
        decode = { ctx, buffer, version ->
          check(version == 1) { "unsupported version" }
          Int2ObjectOpenHashMapCodec.decode(ctx, buffer) { buffer -> IntArrayListCodec.decode(ctx, buffer) }
        },
      )
    }
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
    return labelHash2VertexId.getInt(hash)
  }

  override fun getLabelByVertexId(id: ID): Label? = id2Compact.get(id)?.label
  override fun getVertexCompactById(id: ID): BazelTargetCompact? = id2Compact.get(id)

  override fun getEdgeById(id: ID): BazelTargetEdge? = id2Edge.get(id)

  override fun getSuccessors(id: ID): IntList = id2Successors.get(id) ?: IntLists.EMPTY_LIST

  override fun getPredecessors(id: ID): IntList = id2Predecessors.get(id) ?: IntLists.EMPTY_LIST

  override fun getOutgoingEdges(id: ID): IntList {
    val edges = IntArrayList()
    getSuccessors(id)
      .map { getEdgeBetween(id, it) }
      .filter { it != EMPTY_ID }
      .forEach { edges.add(it) }
    return edges
  }

  override fun getIncomingEdges(id: ID): IntList {
    val edges = IntArrayList()
    getPredecessors(id)
      .map { getEdgeBetween(it, id) }
      .filter { it != EMPTY_ID }
      .forEach { edges.add(it) }
    return edges
  }

  override fun getEdgeBetween(
    from: ID,
    to: ID,
  ): ID {
    return edgeLink2EdgeId[packEdgeLink(from, to)] ?: EMPTY_ID
  }

  override fun addVertex(vertex: BazelTargetVertex) {
    id2Vertex.put(vertex.vertexId, vertex)
    id2Compact.put(vertex.vertexId, vertex.toTargetCompact())

    val labelHash = hash { putResolvedLabel(vertex.label as ResolvedLabel) }
    labelHash2VertexId.put(labelHash, vertex.vertexId)
  }

  override fun addEdge(edge: BazelTargetEdge) {
    id2Edge.put(edge.edgeId, edge)
    id2Successors.computeIfAbsent(edge.from) { IntArrayList() }.add(edge.to)
    id2Predecessors.computeIfAbsent(edge.to) { IntArrayList() }.add(edge.from)

    edgeLink2EdgeId[packEdgeLink(edge.from, edge.to)] = edge.edgeId
  }

  override fun removeVertexById(
    id: ID,
  ): BazelTargetVertex? {
    val vertex = id2Vertex.remove(key = id, useReturn = true) ?: return null
    id2Compact.remove(id)

    val labelHash = hash { putResolvedLabel(vertex.label as ResolvedLabel) }
    labelHash2VertexId.removeInt(labelHash)

    val successorIds = id2Successors.get(vertex.vertexId)
    if (successorIds != null) {
      for (n in successorIds.indices) {
        val successor = successorIds.getInt(n)
        id2Predecessors.remove(successor, vertex.vertexId)
        val link = removeLinksBetween(vertex.vertexId, successor)
        if (link != EMPTY_ID) {
          id2Edge.remove(link)
        }
      }
    }

    val predecessorIds = id2Predecessors.get(vertex.vertexId)
    if (predecessorIds != null) {
      for (n in predecessorIds.indices) {
        val predecessor = predecessorIds.getInt(n)
        id2Successors.remove(predecessor, vertex.vertexId)
        val link = removeLinksBetween(predecessor, vertex.vertexId)
        if (link != EMPTY_ID) {
          id2Edge.remove(link)
        }
      }
    }

    id2Successors.remove(vertex.vertexId)
    id2Predecessors.remove(vertex.vertexId)

    return vertex
  }

  override fun removeEdgeById(
    id: ID,
  ): BazelTargetEdge? {
    val edge = id2Edge.remove(key = id, useReturn = true) ?: return null
    id2Successors.remove(edge.from, edge.to)
    id2Predecessors.remove(edge.to, edge.from)

    edgeLink2EdgeId.remove(packEdgeLink(edge.from, edge.to))
    return edge
  }

  override fun getAllVertexIds(): IntSet = id2Successors.keys

  override fun getNextVertexId(): ID = metadata.modify { it.copy(vertexIdCounter = it.vertexIdCounter + 1) }.vertexIdCounter

  override fun getNextEdgeId(): ID = metadata.modify { it.copy(vertexIdCounter = it.edgeIdCounter + 1) }.edgeIdCounter

  override fun clear() {
    id2Vertex.clear()
    id2Edge.clear()
    id2Compact.clear()
    labelHash2VertexId.clear()
    edgeLink2EdgeId.clear()
    id2Successors.clear()
    id2Predecessors.clear()
  }

  private fun removeLinksBetween(from: ID, to: ID): ID {
    return edgeLink2EdgeId.remove(packEdgeLink(from, to)) ?: EMPTY_ID
  }

  private fun BazelTargetVertex.toTargetCompact(): BazelTargetCompact {
    return BazelTargetCompact(
      label = label,
      vertexId = vertexId,
      isExecutable = BazelTargetTag.EXECUTABLE in genericData.tags,
    )
  }

  private fun packEdgeLink(from: Int, to: Int): Long = (from.toLong() shl 32) or to.toLong()

}
