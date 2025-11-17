package org.jetbrains.bazel.sync_new.graph.impl

import com.dynatrace.hash4j.hashing.HashValue128
import com.jetbrains.bazel.sync_new.proto.BazelTargetCompactProto
import com.jetbrains.bazel.sync_new.proto.BazelTargetEdgeProto
import com.jetbrains.bazel.sync_new.proto.BazelTargetGenericData
import com.jetbrains.bazel.sync_new.proto.BazelTargetVertexProto
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import it.unimi.dsi.fastutil.longs.LongLists
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.sync_new.codec.ofLabel
import org.jetbrains.bazel.sync_new.codec.ofLong
import org.jetbrains.bazel.sync_new.codec.proto.ofProtoMessage
import org.jetbrains.bazel.sync_new.codec.withConverter
import org.jetbrains.bazel.sync_new.graph.ID
import org.jetbrains.bazel.sync_new.graph.TargetGraph
import org.jetbrains.bazel.sync_new.graph.collect.forEachFast
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStorage
import org.jetbrains.bazel.sync_new.storage.createSortedKVStorage
import org.jetbrains.bazel.sync_new.storage.getValue
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.putResolvedLabel

class BazelTargetGraph(val storage: StorageContext) : TargetGraph<BazelTargetVertex, BazelTargetEdge, BazelTargetCompact> {
  private val id2Vertex =
    storage.createSortedKVStorage<ID, BazelTargetVertex>("bazel.target.graph.id2vertex", StorageHints.USE_PAGED_STORE)
      .withKeyComparator { compareBy { it } }
      .withKeyCodec { ofLong() }
      .withValueCodec {
        ofProtoMessage<BazelTargetVertexProto>()
          .withConverter(BazelTargetVertex.converter)
      }
      .build()

  private val id2Edge =
    storage.createSortedKVStorage<ID, BazelTargetEdge>("bazel.target.graph.id2edge", StorageHints.USE_PAGED_STORE)
      .withKeyComparator { compareBy { it } }
      .withKeyCodec { ofLong() }
      .withValueCodec {
        ofProtoMessage<BazelTargetEdgeProto>()
          .withConverter(BazelTargetEdge.converter)
      }
      .build()

  private val id2Compact =
    storage.createSortedKVStorage<ID, BazelTargetCompact>("bazel.target.graph.id2label", StorageHints.USE_PAGED_STORE)
      .withKeyComparator { compareBy { it } }
      .withKeyCodec { ofLong() }
      .withValueCodec {
        ofProtoMessage<BazelTargetCompactProto>()
          .withConverter(BazelTargetCompact.converter)
      }
      .build()

  private val labelHash2VertexId by storage.createFlatStorage<Hash2IdWire>(
    "bazel.target.graph.labelHash2vertexId",
    StorageHints.USE_IN_MEMORY,
  )
    .withCreator { Hash2IdWire() }
    .withCodec { Hash2IdWire.codec }
    .build()

  private val edgeLink2EdgesId by storage.createFlatStorage<Hash2ListIdWire>(
    "bazel.target.graph.edgeLink2EdgesId",
    StorageHints.USE_IN_MEMORY,
  )
    .withCreator { Hash2ListIdWire() }
    .withCodec { Hash2ListIdWire.codec }
    .build()

  private val id2Successors by storage.createFlatStorage<Id2ListIdWire>("bazel.target.graph.id2Successors", StorageHints.USE_IN_MEMORY)
    .withCreator { Id2ListIdWire() }
    .withCodec { Id2ListIdWire.codec }
    .build()

  private val id2Predecessors by storage.createFlatStorage<Id2ListIdWire>("bazel.target.graph.id2Predecessors", StorageHints.USE_IN_MEMORY)
    .withCreator { Id2ListIdWire() }
    .withCodec { Id2ListIdWire.codec }
    .build()

  override val vertices: Sequence<BazelTargetVertex>
    get() = id2Vertex.values()
  override val edges: Sequence<BazelTargetEdge>
    get() = id2Edge.values()

  override fun getVertexById(id: ID): BazelTargetVertex? = id2Vertex.get(id)

  override fun getVertexByLabel(label: Label): BazelTargetVertex? {
    val hash = hash { putResolvedLabel(label as ResolvedLabel) }
    return id2Vertex.get(labelHash2VertexId.map.getLong(hash))
  }

  override fun getLabelByVertexId(id: ID): Label? = id2Compact.get(id)?.label
  override fun getTargetCompactById(id: ID): BazelTargetCompact? {
    TODO("Not yet implemented")
  }

  override fun getEdgeById(id: ID): BazelTargetEdge? = id2Edge.get(id)

  override fun getSuccessors(id: ID): LongList = id2Successors.get(id) ?: LongLists.EMPTY_LIST

  override fun getPredecessors(id: ID): LongList = id2Predecessors.get(id) ?: LongLists.EMPTY_LIST

  override fun getOutgoingEdges(id: ID): LongList {
    val edges = LongArrayList()
    getSuccessors(id).forEachFast { edges.addAll(getEdgesBetween(id, it)) }
    return edges
  }

  override fun getIncomingEdges(id: ID): LongList {
    val edges = LongArrayList()
    getPredecessors(id).forEachFast { edges.addAll(getEdgesBetween(it, id)) }
    return edges
  }

  override fun getEdgesBetween(
    from: ID,
    to: ID,
  ): LongList {
    val edges = edgeLink2EdgesId.map[HashValue128(from, to)]
    if (edges.isNullOrEmpty()) {
      return LongLists.EMPTY_LIST
    }
    return edges
  }

  override fun addVertex(vertex: BazelTargetVertex) {
    id2Vertex.set(vertex.id, vertex)
    id2Compact.set(vertex.id, vertex.toTargetCompact())

    val labelHash = hash { putResolvedLabel(vertex.label as ResolvedLabel) }
    labelHash2VertexId.map.put(labelHash, vertex.id)
  }

  override fun addEdge(edge: BazelTargetEdge) {
    id2Edge.set(edge.id, edge)
    id2Successors.add(edge.from, edge.to)
    id2Predecessors.add(edge.to, edge.from)

    val linkHashValue = HashValue128(edge.from, edge.to)
    edgeLink2EdgesId.map.computeIfAbsent(linkHashValue) { LongArrayList() }.add(edge.id)
  }

  override fun removeVertexById(
    id: ID,
  ): BazelTargetVertex? {
    val vertex = id2Vertex.remove(key = id, useReturn = true) ?: return null
    id2Compact.remove(id)

    val labelHash = hash { putResolvedLabel(vertex.label as ResolvedLabel) }
    labelHash2VertexId.map.removeLong(labelHash)

    val successorIds = id2Successors.map.remove(vertex.id)
    for (n in successorIds.indices) {
      val successor = successorIds.getLong(n)
      id2Predecessors.remove(successor, vertex.id)
      removeLinksBetween(vertex.id, successor).forEachFast { id2Edge.remove(it) }
    }

    val predecessorIds = id2Predecessors.map.remove(vertex.id)
    for (n in predecessorIds.indices) {
      val predecessor = predecessorIds.getLong(n)
      id2Successors.remove(predecessor, vertex.id)
      removeLinksBetween(predecessor, vertex.id).forEachFast { id2Edge.remove(it) }
    }

    return vertex
  }

  override fun removeEdgeById(
    id: ID,
  ): BazelTargetEdge? {
    val edge = id2Edge.remove(key = id, useReturn = true) ?: return null
    id2Successors.remove(edge.from, edge.to)
    id2Predecessors.remove(edge.to, edge.from)

    val linkHash = HashValue128(edge.from, edge.to)
    edgeLink2EdgesId.map[linkHash]?.removeIf { it == edge.id }
    return edge
  }

  override fun getNextVertexId(): ID = getUsableId { id2Vertex.getHighestKey() }

  override fun getNextEdgeId(): ID = getUsableId { id2Edge.getHighestKey() }

  // id `0` is reserved as an invalid state or absence of value
  private fun getUsableId(getter: () -> Long?): Long {
    val id = getter() ?: return 1
    return maxOf(id + 1, 1)
  }

  private fun removeLinksBetween(form: ID, to: ID): LongList {
    val linkHash = HashValue128(form, to)
    return edgeLink2EdgesId.map[linkHash] ?: LongLists.EMPTY_LIST
  }

  private fun BazelTargetVertex.toTargetCompact(): BazelTargetCompact {
    val isExecutable = if (proto.hasGenericData()) {
      proto.genericData.tagsList.contains(BazelTargetGenericData.Tags.EXECUTABLE)
    } else {
      false
    }
    val proto = BazelTargetCompactProto.newBuilder()
      .setVertexId(id)
      .setCanonicalLabel(proto.canonicalLabel)
      .setIsExecutable(isExecutable)
      .build()
    return BazelTargetCompact(proto)
  }

}
