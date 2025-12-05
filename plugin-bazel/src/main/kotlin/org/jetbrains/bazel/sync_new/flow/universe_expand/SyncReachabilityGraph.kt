package org.jetbrains.bazel.sync_new.flow.universe_expand

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntCollection
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntLists
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.Int2ObjectOpenHashMapCodec
import org.jetbrains.bazel.sync_new.codec.IntListCodec
import org.jetbrains.bazel.sync_new.codec.IntOpenHashSetCodec
import org.jetbrains.bazel.sync_new.codec.LabelCodec
import org.jetbrains.bazel.sync_new.codec.Object2IntOpenHashMapCodec
import org.jetbrains.bazel.sync_new.codec.versionedCodecOf

// TODO: serialize id2Label and label2id more efficiently
// TODO: get rid of id2Predecessors
class SyncReachabilityGraph {
  companion object {
    internal val codec: Codec<SyncReachabilityGraph> = versionedCodecOf(
      version = 1,
      encode = { ctx, buffer, value ->
        buffer.writeVarInt(value.idCounter)
        IntOpenHashSetCodec.encode(ctx, buffer, value.universeVertices)
        Int2ObjectOpenHashMapCodec.encode(ctx, buffer, value.id2Label) { buffer, value -> LabelCodec.encode(ctx, buffer, value) }
        Object2IntOpenHashMapCodec.encode(ctx, buffer, value.label2Id) { buffer, value -> LabelCodec.encode(ctx, buffer, value) }
        Int2ObjectOpenHashMapCodec.encode(ctx, buffer, value.id2Successors) { buffer, value -> IntListCodec.encode(ctx, buffer, value) }
        Int2ObjectOpenHashMapCodec.encode(ctx, buffer, value.id2Predecessors) { buffer, value -> IntListCodec.encode(ctx, buffer, value) }
      },
      decode = { ctx, buffer, version ->
        check(version == 1) { "unsupported version" }

        val graph = SyncReachabilityGraph()
        graph.idCounter = buffer.readVarInt()
        graph.universeVertices = IntOpenHashSetCodec.decode(ctx, buffer)
        graph.id2Label = Int2ObjectOpenHashMapCodec.decode(ctx, buffer) { buffer -> LabelCodec.decode(ctx, buffer) }
        graph.label2Id = Object2IntOpenHashMapCodec.decode(ctx, buffer) { buffer -> LabelCodec.decode(ctx, buffer) }
        graph.id2Successors = Int2ObjectOpenHashMapCodec.decode(ctx, buffer) { buffer -> IntListCodec.decode(ctx, buffer) }
        graph.id2Predecessors = Int2ObjectOpenHashMapCodec.decode(ctx, buffer) { buffer -> IntListCodec.decode(ctx, buffer) }
        graph
      },
    )

    const val EMPTY_ID: Int = 0
  }

  // counter used for id -> label mapping generation
  // mainly used for performance reasons
  internal var idCounter: Int = 1

  // input diff only updates this set,
  // using this property, we can assume that those nodes represent
  // source subgraph for reachability resolution
  // in simpler terms all ids in this set reflect targets defined by
  // target patterns in project view
  internal var universeVertices: IntOpenHashSet = IntOpenHashSet()
  internal var id2Label: Int2ObjectOpenHashMap<Label> = Int2ObjectOpenHashMap()
  internal var label2Id: Object2IntOpenHashMap<Label> = Object2IntOpenHashMap()
  internal var id2Successors: Int2ObjectOpenHashMap<IntList> = Int2ObjectOpenHashMap()
  internal var id2Predecessors: Int2ObjectOpenHashMap<IntList> = Int2ObjectOpenHashMap()

  fun addUniverseVertex(label: Label) {
    universeVertices.add(getOrAddVertex(label))
  }

  fun removeUniverseVertex(label: Label) {
    val id = label2Id.getInt(label)
    if (id != EMPTY_ID) {
      universeVertices.remove(id)
    }
  }

  fun hasVertex(label: Label): Boolean = label2Id.containsKey(label)

  fun getOrAddVertex(label: Label): Int {
    val existing = label2Id.getInt(label)
    if (existing != EMPTY_ID) {
      return existing
    }
    val id = idCounter++
    id2Label[id] = label
    label2Id[label] = id
    return id
  }

  fun removeVertexByLabel(label: Label) {
    val id = label2Id.getInt(label)
    if (id != EMPTY_ID) {
      removeVertex(id)
    }
  }

  fun removeVertex(id: Int): Label? {
    val label = id2Label.remove(id) ?: return null
    label2Id.removeInt(label)

    val successors = id2Successors.get(id)
    if (successors != null) {
      for (n in successors.indices) {
        val predecessors = id2Predecessors.get(successors.getInt(n))
        if (predecessors != null) {
          predecessors.removeIf { it == id }
        }
      }
    }

    val predecessors = id2Predecessors.get(id)
    if (predecessors != null) {
      for (n in predecessors.indices) {
        val successors = id2Successors.get(predecessors.getInt(n))
        if (successors != null) {
          successors.removeIf { it == id }
        }
      }
    }

    id2Successors.remove(id)
    id2Predecessors.remove(id)

    return label
  }

  fun addEdge(from: Int, to: Int) {
    val successors = id2Successors.get(from) ?: IntArrayList()
    successors.add(to)
    id2Successors[from] = successors

    val predecessors = id2Predecessors.get(to) ?: IntArrayList()
    predecessors.add(from)
    id2Predecessors[to] = predecessors
  }

  fun removeEdge(from: Int, to: Int) {
    val successors = id2Successors.get(from) ?: return
    successors.removeIf { it == to }
    id2Successors[from] = successors

    val predecessors = id2Predecessors.get(to) ?: return
    predecessors.removeIf { it == from }
    id2Predecessors[to] = predecessors
  }

  fun getPredecessors(id: Int): IntList = id2Predecessors.get(id) ?: IntLists.EMPTY_LIST
  fun getSuccessors(id: Int): IntList = id2Successors.get(id) ?: IntLists.EMPTY_LIST

  fun getPredecessorsLabels(label: Label): Set<Label> = getPredecessors(label2Id.getInt(label))
    .mapNotNull { id2Label[it] }
    .toSet()

  fun getSuccessorsLabels(label: Label): Set<Label> = getSuccessors(label2Id.getInt(label))
    .mapNotNull { id2Label[it] }
    .toSet()

  fun computeUnreachableVertices(): IntOpenHashSet {
    val reachable = IntOpenHashSet(id2Successors.keys)
    val visited = IntOpenHashSet()
    val queue = ArrayDeque<Int>() // ultimate fastutil does not have IntArrayFIFOQueue :(
    queue.addAll(universeVertices)
    while (!queue.isEmpty()) {
      val element = queue.removeFirst()
      if (!visited.add(element)) {
        continue
      }
      val successors = id2Successors.get(element)
      if (successors != null) {
        queue.addAll(successors)
      }
    }
    // unreachable => A / B
    // where A = all vertices
    //       B = all vertices reachable from sync universe
    reachable.removeAll(visited)
    return reachable
  }

  fun removeAllVertices(ids: IntCollection) {
    val iter = ids.intIterator()
    while (iter.hasNext()) {
      removeVertex(iter.nextInt())
    }
  }

}
