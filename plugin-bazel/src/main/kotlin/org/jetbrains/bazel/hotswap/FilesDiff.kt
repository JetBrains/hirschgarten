package org.jetbrains.bazel.hotswap

import org.jetbrains.bazel.hotswap.FilesDiff.Companion.diffFiles
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.getLastModifiedTime

/**
 * A data class representing the diff between two sets of files.
 *
 * The type of file in the two sets can be different (F vs S), with a one-way F to S mapping
 * provided in [diffFiles].
 * @param newFileState maps a jar file to its last modified time
 * @param updatedFiles provides a list of updated jar files, which will be used for constructing the class manifest
 * @param removedFiles keeps track of the list of removed jar files, this is used for writing unit test to check for correctness.
 */
data class FilesDiff(
  val newFileState: Map<Path, FileTime>,
  val updatedFiles: List<Path>,
  val removedFiles: Set<Path>,
) {
  companion object {
    /** Diffs a collection of files based on timestamps. */
    fun diffFileTimestamps(oldState: Map<Path, FileTime>?, files: Collection<Path>): FilesDiff {
      val newState = files.associateWith { it.getLastModifiedTime() }
      return diffFiles(oldState, newState)
    }

    fun diffFiles(oldState: Map<Path, FileTime>?, newState: Map<Path, FileTime>): FilesDiff {
      // Find changed/new files
      val previous = oldState ?: mapOf()
      val updated =
        newState.entries
          .filter { (key, value) ->
            value != previous[key]
          }.map { it.key }

      // Find removed files
      val removed = HashSet(previous.keys)
      newState.keys.forEach { removed.remove(it) }

      return FilesDiff(
        newFileState = newState,
        updatedFiles = updated,
        removedFiles = removed,
      )
    }
  }
}
