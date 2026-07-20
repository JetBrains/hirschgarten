package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path

internal val NOT_LOADED_SOURCE_FILE_COLLECTION: SourceFileCollection = object : SourceFileCollection {
  override fun isEmpty(): Boolean = notLoaded()
  override fun getFiles(): Sequence<Path> = notLoaded()
  private fun notLoaded(): Nothing = throw IllegalStateException("source files were not loaded")
}

internal val NOT_LOADED_DEPS: List<DependencyLabel> = object : AbstractList<DependencyLabel>() {
  override val size: Int get() = notLoaded()
  override fun get(index: Int): DependencyLabel = notLoaded()
  private fun notLoaded(): Nothing = throw IllegalStateException("dependencies were not loaded")
}

internal val NOT_LOADED_DATA: List<BuildTargetData> = object : AbstractList<BuildTargetData>() {
  override val size: Int get() = notLoaded()
  override fun get(index: Int): BuildTargetData = notLoaded()
  private fun notLoaded(): Nothing = throw IllegalStateException("target data was not loaded")
}
