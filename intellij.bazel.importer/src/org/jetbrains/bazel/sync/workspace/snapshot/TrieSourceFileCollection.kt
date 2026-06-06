package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

/**
 * Traditional path trie paths under [relativizeRoot] share their common prefix through
 * [TrieNode] children paths outside [relativizeRoot] are kept in [externalFiles].
 */
@ApiStatus.Internal
class TrieSourceFileCollection(
  val relativizeRoot: Path?,
  val root: TrieNode,
  val externalFiles: List<Path>,
) : SourceFileCollection {
  override fun isEmpty(): Boolean = externalFiles.isEmpty() && root.isEmpty()

  override fun getFiles(): Sequence<Path> =
    externalFiles.asSequence() + traverseRelativeTrie()

  private fun traverseRelativeTrie(): Sequence<Path> = sequence {
    val rootPath = relativizeRoot ?: return@sequence
    val stack = ArrayDeque<Pair<Path, TrieNode>>()
    stack.addLast(rootPath to root)
    while (stack.isNotEmpty()) {
      val (path, node) = stack.removeLast()
      if (node.isTerminal) {
        yield(path)
      }
      for (child in node.children) {
        stack.addLast(path.resolve(child.segment) to child)
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is TrieSourceFileCollection) {
      return false
    }
    return relativizeRoot == other.relativizeRoot
           && getFiles().toSet() == other.getFiles().toSet()
  }

  override fun hashCode(): Int = 31 * relativizeRoot.hashCode() + getFiles().toSet().hashCode()
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

@ApiStatus.Internal
object SourceFileCollectionBuilder {
  fun build(relativeRoot: Path?, paths: Iterable<Path>): SourceFileCollection = buildImpl(relativeRoot, paths)
  fun build(relativeRoot: Path?, paths: Sequence<Path>): SourceFileCollection = buildImpl(relativeRoot, paths.asIterable())

  fun build(paths: Iterable<Path>): SourceFileCollection = buildImpl(relativeRoot = null, paths = paths)

  private fun buildImpl(relativeRoot: Path? = null, paths: Iterable<Path>): SourceFileCollection {
    val (relativizablePaths, externalPaths) = paths.partition { relativeRoot != null && it.startsWith(relativeRoot) }
      .let { (l, r) -> l to r.toMutableList() }

    if (relativizablePaths.isEmpty() && externalPaths.isEmpty()) {
      return SourceFileCollection.EMPTY
    }

    val root = TrieNode(segment = "")
    for (path in relativizablePaths) {
      val relativePath = path.relativeToOrNull(relativeRoot ?: continue)
      if (relativePath == null) {
        externalPaths.add(path)
        continue
      }
      var node = root
      for (n in 0 until relativePath.nameCount) {
        val segmentStr = relativePath.getName(n).toString()
        node = node.findOrInsertChild(segmentStr)
      }
      node.isTerminal = true
    }

    return TrieSourceFileCollection(
      relativizeRoot = relativeRoot,
      root = root,
      externalFiles = externalPaths,
    )
  }
}
