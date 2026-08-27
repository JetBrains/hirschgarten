package org.jetbrains.bazel.sync.workspace.snapshot

import com.google.devtools.intellij.aspect.Common
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection

internal fun OutputLocation.root(): OutputLocation = withPath(path = "")

internal fun OutputLocation.withPath(path: String): OutputLocation = when (this) {
  is OutputLocation.Workspace -> copy(relativePath = path)
  is OutputLocation.Output -> copy(relativePath = path)
  is OutputLocation.External -> copy(relativePath = path)
  is OutputLocation.Host -> copy(absolutePath = path)
}

internal fun OutputLocation.path(): String = when (this) {
  is OutputLocation.Workspace -> relativePath
  is OutputLocation.Output -> relativePath
  is OutputLocation.External -> relativePath
  is OutputLocation.Host -> absolutePath
}

// roots map key is always `OutputLocation` with empty path string
internal class TrieOutputLocationCollection(val roots: Map<OutputLocation, PathsTrie>) : OutputLocationCollection {
  override fun isEmpty(): Boolean = roots.all { (_, trie) -> trie.isEmpty() }

  override fun getOutputLocations(): Sequence<OutputLocation> =
    roots.asSequence().flatMap { (root, trie) -> trie.paths().map { root.withPath(it) } }

  override fun equals(other: Any?): Boolean =
    this === other || (other is OutputLocationCollection && getOutputLocations().toSet() == other.getOutputLocations().toSet())

  override fun hashCode(): Int = getOutputLocations().toSet().hashCode()
}

@ApiStatus.Internal
object OutputLocationCollectionBuilder {
  fun build(locations: Iterable<Common.ArtifactLocation>): OutputLocationCollection =
    ofLocations(locations.map(OutputLocation::parse))

  fun buildExecroot(locations: Iterable<String>): OutputLocationCollection =
    ofLocations(locations.map(OutputLocation::parseExecrootPath))

  private fun ofLocations(locations: List<OutputLocation>): OutputLocationCollection {
    if (locations.isEmpty()) {
      return OutputLocationCollection.EMPTY
    }
    val roots = hashMapOf<OutputLocation, PathsTrie>()
    for (location in locations) {
      roots.getOrPut(location.root()) { PathsTrie() }
        .insertPath(location.path())
    }
    return TrieOutputLocationCollection(roots)
  }
}
