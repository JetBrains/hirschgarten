package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus

/**
 * Traditional path trie, the paths share their common prefix through [TrieNode] children.
 */
@ApiStatus.Internal
class PathsTrie(val root: TrieNode = TrieNode(segment = "")) {
  fun isEmpty(): Boolean = root.isEmpty()

  fun insert(segments: Iterable<String>) {
    var node = root
    for (segment in segments) {
      node = node.findOrInsertChild(segment)
    }
    node.isTerminal = true
  }

  fun insertPath(path: String) {
    insert(path.split(SEPARATOR))
  }

  fun paths(): Sequence<String> = sequence {
    // `null` marks trie root
    val stack = ArrayDeque<Pair<String?, TrieNode>>()
    stack.addLast(null to root)
    while (stack.isNotEmpty()) {
      val (path, node) = stack.removeLast()
      if (node.isTerminal) {
        yield(path ?: "")
      }
      for (child in node.children) {
        val childPath = if (path == null) child.segment else path + SEPARATOR + child.segment
        stack.addLast(childPath to child)
      }
    }
  }

  companion object {
    const val SEPARATOR: String = "/"
  }
}

@ApiStatus.Internal
class TrieNode(
  val segment: String,
  val children: MutableList<TrieNode> = ArrayList(),
  var isTerminal: Boolean = false,
) {
  fun isEmpty(): Boolean = !isTerminal && children.isEmpty()

  fun findChild(segment: String): TrieNode? {
    val idx = children.binarySearch { it.segment.compareTo(segment) }
    return if (idx >= 0) children[idx] else null
  }

  fun findOrInsertChild(segment: String): TrieNode {
    val idx = children.binarySearch { it.segment.compareTo(segment) }
    if (idx >= 0) {
      return children[idx]
    }
    val node = TrieNode(segment)
    children.add(-(idx + 1), node)
    return node
  }
}
