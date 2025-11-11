package org.jetbrains.bazel.sync_new.graph

import com.jetbrains.bazel.sync_new.proto.BazelTargetNode
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongSet
import org.jetbrains.bazel.sync_new.codec.ofPrimitiveLong
import org.jetbrains.bazel.sync_new.codec.proto.ofProtoMessage
import org.jetbrains.bazel.sync_new.codec.withConverter
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.bsearch.bsearchOfLong
import org.jetbrains.bazel.sync_new.storage.createFlatStorage
import org.jetbrains.bazel.sync_new.storage.createKVStorage

class BazelTargetGraph(val storageContext: StorageContext) : ReferenceDirectedGraph<Long, BazelTargetGraphVertex, BazelTargetGraphEdge> {
  private val id2Vertex =
    storageContext.createKVStorage<Long, BazelTargetGraphVertex>("bazel.target.graph.id2vertex", StorageHints.USE_PAGED_STORE)
      .withKeyCodec({ ofPrimitiveLong() }, { bsearchOfLong() })
      .withValueCodec {
        ofProtoMessage<BazelTargetNode>()
          .withConverter(BazelTargetGraphVertex.converter)
      }
      .build()

  private val id2Successors =
    storageContext.createFlatStorage<Long2ObjectMap<LongSet>>("bazel.target.graph.outgoingEdges", StorageHints.USE_IN_MEMORY)
      .withCreator { Long2ObjectOpenHashMap() }
      .withCodec { }
      .build()

  private val id2Predecessors =
    storageContext.createFlatStorage<Long2ObjectMap<LongSet>>("bazel.target.graph.outgoingEdges", StorageHints.USE_IN_MEMORY)
      .withCreator { Long2ObjectOpenHashMap() }
      .withCodec { }
      .build()

  override val isAcyclic: Boolean = true
  override val vertices: Sequence<BazelTargetGraphVertex>
    get() = id2Vertex.values()
  override val edges: Sequence<BazelTargetGraphEdge>
    get() = error("not supported")

  override fun getOutgoingEdges(vertex: BazelTargetGraphVertex): Sequence<BazelTargetGraphEdge> {
    TODO("Not yet implemented")
  }

  override fun getIncomingEdges(vertex: BazelTargetGraphVertex): Sequence<BazelTargetGraphEdge> {
    TODO("Not yet implemented")
  }

  override fun getSuccessors(vertex: BazelTargetGraphVertex): Sequence<BazelTargetGraphVertex> {
    TODO("Not yet implemented")
  }

  override fun getPredecessors(vertex: BazelTargetGraphVertex): Sequence<BazelTargetGraphVertex> {
    TODO("Not yet implemented")
  }

  override fun getVertexById(id: Long): BazelTargetGraphVertex? {
    TODO("Not yet implemented")
  }

  override fun getVertexId(vertex: BazelTargetGraphVertex): Long? {
    TODO("Not yet implemented")
  }

  override fun getOutgoingEdgesWithIds(id: Long): Sequence<BazelTargetGraphEdge> {
    TODO("Not yet implemented")
  }

  override fun getIncomingEdgesWithIds(id: Long): Sequence<BazelTargetGraphEdge> {
    TODO("Not yet implemented")
  }

  override fun getSuccessorsWithIds(id: Long): Sequence<Long> {
    TODO("Not yet implemented")
  }

  override fun getPredecessorsWithIds(id: Long): Sequence<Long> {
    TODO("Not yet implemented")
  }
}
