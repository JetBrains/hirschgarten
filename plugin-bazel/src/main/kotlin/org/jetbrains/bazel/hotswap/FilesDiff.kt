package org.jetbrains.bazel.hotswap

import org.jetbrains.bazel.hotswap.FilesDiff.Companion.diffFiles
import java.io.File

/**
 * A data class representing the diff between two sets of files.
 *
 * The type of file in the two sets can be different (F vs S), with a one-way F to S mapping
 * provided in [diffFiles].
 */
data class FilesDiff(
  val newFileState: Map<File, Long>,
  val updatedFiles: List<File>,
  val removedFiles: Set<File>,
) {
  companion object {
    /** Diffs a collection of files based on timestamps. */
    fun diffFileTimestamps(oldState: Map<File, Long>?, files: Collection<File>): FilesDiff {
      val newState = files.associateWith { it.lastModified() }
      return diffFiles(oldState, newState)
    }

    fun diffFiles(oldState: Map<File, Long>?, newState: Map<File, Long>): FilesDiff {
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
