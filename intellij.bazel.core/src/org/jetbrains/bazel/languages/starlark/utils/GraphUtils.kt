package org.jetbrains.bazel.languages.starlark.utils

internal object GraphUtils {
  // Tarjan's algorithm for computing cyclic strongly connected components.
  fun <T> findCyclicStronglyConnectedComponents(
    nodes: Collection<T>,
    edges: (T) -> Collection<T>,
  ): List<List<T>> {
    var nextIndex = 0
    val indexByNode = HashMap<T, Int>()
    val lowLinkByNode = HashMap<T, Int>()
    val stack = ArrayDeque<T>()
    val onStack = HashSet<T>()
    val components = mutableListOf<List<T>>()

    fun visit(node: T) {
      indexByNode[node] = nextIndex
      lowLinkByNode[node] = nextIndex
      nextIndex++

      stack.addLast(node)
      onStack.add(node)

      for (target in edges(node)) {
        if (target !in indexByNode) {
          visit(target)
          lowLinkByNode[node] = minOf(lowLinkByNode.getValue(node), lowLinkByNode.getValue(target))
        }
        else if (target in onStack) {
          lowLinkByNode[node] = minOf(lowLinkByNode.getValue(node), indexByNode.getValue(target))
        }
      }

      if (lowLinkByNode.getValue(node) == indexByNode.getValue(node)) {
        val component = mutableListOf<T>()
        do {
          val target = stack.removeLast()
          onStack.remove(target)
          component.add(target)
        } while (target != node)

        val hasSelfLoop = component.size == 1 && component.single() in edges(component.single())
        if (component.size > 1 || hasSelfLoop) components.add(component)
      }
    }

    nodes.forEach{ if (it !in indexByNode) visit(it) }

    return components
  }
}
