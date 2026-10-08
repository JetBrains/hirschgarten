package org.jetbrains.bazel.sync.workspace.snapshot

import com.google.devtools.intellij.aspect.Common
import com.intellij.util.containers.Interner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationCollection
import org.jetbrains.bsp.protocol.OutputLocationParser
import org.jetbrains.bsp.protocol.contentHash

internal class InterningOutputLocationCollection private constructor(
  internal val locations: List<OutputLocation>,
) : OutputLocationCollection {
  private val contentHash by lazy(LazyThreadSafetyMode.PUBLICATION) { contentHash(locations) }

  override fun isEmpty(): Boolean = locations.isEmpty()

  override fun getOutputLocations(): Sequence<OutputLocation> = locations.asSequence()

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other is InterningOutputLocationCollection) {
      return locations.size == other.locations.size && contentHash == other.contentHash
    }
    return false
  }

  override fun hashCode(): Int = contentHash.asInt

  companion object {
    private val interner = Interner.createWeakInterner<OutputLocation>()

    fun create(locations: Iterable<OutputLocation>): InterningOutputLocationCollection =
      InterningOutputLocationCollection(locations.map { interner.intern(it) })
  }
}

@ApiStatus.Internal
object OutputLocationCollectionBuilder {

  suspend fun build(locations: List<Common.ArtifactLocation>, parser: OutputLocationParser): OutputLocationCollection =
    ofLocations(parser.parse(locations))

  suspend fun buildExecroot(locations: List<String>, parser: OutputLocationParser): OutputLocationCollection =
    ofLocations(parser.parseExecrootPath(locations))

  fun ofLocations(locations: List<OutputLocation>): OutputLocationCollection =
    if (locations.isEmpty()) OutputLocationCollection.EMPTY else InterningOutputLocationCollection.create(locations)
}
