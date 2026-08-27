package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

/**
 * Paths under [relativizeRoot] are kept in [trie], paths outside [relativizeRoot] are kept in [externalFiles].
 */
@ApiStatus.Internal
class TrieSourceFileCollection(
  val relativizeRoot: Path?,
  val trie: PathsTrie,
  val externalFiles: List<Path>,
) : SourceFileCollection {
  override fun isEmpty(): Boolean = externalFiles.isEmpty() && trie.isEmpty()

  override fun getFiles(): Sequence<Path> =
    externalFiles.asSequence() + traverseRelativeTrie()

  private fun traverseRelativeTrie(): Sequence<Path> {
    val rootPath = relativizeRoot ?: return emptySequence()
    return trie.paths().map { rootPath.resolve(it) }
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
object SourceFileCollectionBuilder {

  // only use explicitly defined relativization root only when specific percentage of them matches
  private const val EXPLICIT_RELATIVIZE_ROOT_THRESHOLD = 0.75f

  fun build(relativeRoot: Path?, paths: Iterable<Path>): SourceFileCollection = buildImpl(relativeRoot, paths)
  fun build(relativeRoot: Path?, paths: Sequence<Path>): SourceFileCollection = buildImpl(relativeRoot, paths.asIterable())

  fun build(paths: Iterable<Path>): SourceFileCollection = buildImpl(relativeRoot = null, paths = paths)

  private fun buildImpl(relativeRoot: Path? = null, paths: Iterable<Path>): SourceFileCollection {
    val allPaths = (paths as? List<Path>) ?: paths.toList()
    if (allPaths.isEmpty()) {
      return SourceFileCollection.EMPTY
    }

    val effectiveRoot = if (relativeRoot != null
                            && allPaths.count { it.startsWith(relativeRoot) } >= allPaths.size * EXPLICIT_RELATIVIZE_ROOT_THRESHOLD) {
      relativeRoot
    }
    else {
      commonAncestor(allPaths)
    }

    val trie = PathsTrie()
    val externalPaths = mutableListOf<Path>()
    for (path in allPaths) {
      val relativePath = if (effectiveRoot == null) null else path.relativeToOrNull(effectiveRoot)
      if (relativePath == null) {
        externalPaths.add(path)
        continue
      }
      trie.insert(relativePath.map { it.toString() })
    }

    return TrieSourceFileCollection(
      relativizeRoot = effectiveRoot,
      trie = trie,
      externalFiles = externalPaths,
    )
  }

  // TODO: we can find X amound of best picks and select based in statictic
  private fun commonAncestor(paths: List<Path>): Path? {
    var candidate: Path = paths.first().parent ?: return null
    for (path in paths) {
      var ancestor: Path = candidate
      while (!path.startsWith(ancestor)) {
        ancestor = ancestor.parent ?: return null
      }
      candidate = ancestor
    }
    return candidate
  }
}
