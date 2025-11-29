package org.jetbrains.bazel.sync_new.graph

enum class SimpleGraphTraversalResult {
  CONTINUE,
  SKIP,
  BREAK
}

fun interface SimpleGraphVisitor<V> {
  fun onVisit(vertex: V): SimpleGraphTraversalResult

  companion object {
    fun <V> collect(traverser: (visitor: SimpleGraphVisitor<V>) -> Unit): List<V> {
      val list = mutableListOf<V>()
      traverser(
        SimpleGraphVisitor { vertex ->
          list.add(vertex)
          SimpleGraphTraversalResult.CONTINUE
        },
      )
      return list
    }
  }
}

enum class SimpleGraphTraversalDirection {
  UPWARD, DOWNWARD
}

object SimpleGraphTraverser {
  fun <V> topological(
    graph: SimpleDirectedGraph<V>,
    roots: List<V>,
    visitor: SimpleGraphVisitor<V>,
    direction: SimpleGraphTraversalDirection = SimpleGraphTraversalDirection.DOWNWARD,
  ) {
    val queue = ArrayDeque(roots)
    while (queue.isNotEmpty()) {
      val vertex = queue.removeFirst()
      val result = visitor.onVisit(vertex)
      when (result) {
        SimpleGraphTraversalResult.CONTINUE -> {
          val vertices = when (direction) {
            SimpleGraphTraversalDirection.DOWNWARD -> graph.getSuccessors(vertex)
            SimpleGraphTraversalDirection.UPWARD -> graph.getPredecessors(vertex)
          }
          for (v in vertices) {
            queue.addLast(v)
          }
        }

        SimpleGraphTraversalResult.SKIP -> continue
        SimpleGraphTraversalResult.BREAK -> break
      }
    }
  }
}
