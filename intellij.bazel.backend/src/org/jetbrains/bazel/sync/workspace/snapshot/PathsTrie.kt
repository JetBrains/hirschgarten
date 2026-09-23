package org.jetbrains.bazel.sync.workspace.snapshot

import com.intellij.util.containers.Interner
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
      val children = node.children ?: continue
      for (child in children) {
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
  var children: MutableList<TrieNode>? = null,
  var isTerminal: Boolean = false,
) {
  fun isEmpty(): Boolean = !isTerminal && children?.isEmpty() != false

  fun findChild(segment: String): TrieNode? {
    val children = children ?: return null
    val idx = children.binarySearch { it.segment.compareTo(segment) }
    return if (idx >= 0) children[idx] else null
  }

  fun findOrInsertChild(segment: String): TrieNode {
    val children = getOrCreateChildren()
    val idx = children.binarySearch { it.segment.compareTo(segment) }
    if (idx >= 0) {
      return children[idx]
    }
    val node = TrieNode(interner.intern(segment))
    children.add(-(idx + 1), node)
    return node
  }

  fun getOrCreateChildren(): MutableList<TrieNode> =
    children ?: mutableListOf<TrieNode>().also { children = it }

  private companion object {
    val interner = Interner.createWeakInterner<String>()
  }
}
