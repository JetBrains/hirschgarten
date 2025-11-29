package org.jetbrains.bazel.sync_new.graph

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import org.jetbrains.bazel.sync_new.graph.collect.forEachFast

object GraphTraverser {
  fun <V, E, GRAPH> topologicalSort(
    graph: GRAPH,
    roots: LongList,
    visitor: TopologicalGraphVisitor<V, E, GRAPH>,
  ) where GRAPH : FastDirectedGraph<V, E> {
    val deg = Long2IntOpenHashMap()
    roots.forEachFast { deg.put(it, 0) }
    val queue = LongArrayList()
    queue.addAll(roots)

    while (queue.isNotEmpty()) {
      val vertexId = queue.removeLong(0)
    }
  }
}

interface TopologicalGraphVisitor<V, E, GRAPH>
  where GRAPH : FastDirectedGraph<V, E> {

  fun visit(graph: GRAPH, vertexId: ID, depth: Int): Boolean
}
