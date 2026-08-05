package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Debug
import java.nio.file.Path

/**
 * Unordered file set container
 */
@Debug.Renderer(hasChildren = "!isEmpty()", childrenArray = "arrayOfFiles()")
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

  @Suppress("unused") // used in debug renderer
  fun arrayOfFiles(): Array<Path> = getFiles().toList().toTypedArray()

}
