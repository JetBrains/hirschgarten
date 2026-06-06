package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

// TODO: move to backend module together with `RawBuildTarget`

/**
 * Unordered file set container
 */
@ApiStatus.Internal
interface SourceFileCollection {
  companion object {
    val EMPTY: SourceFileCollection = object : SourceFileCollection {
      override fun isEmpty(): Boolean = true
      override fun getFiles(): Sequence<Path> = sequenceOf()
    }
  }

  fun isEmpty(): Boolean
  fun getFiles(): Sequence<Path>
}
