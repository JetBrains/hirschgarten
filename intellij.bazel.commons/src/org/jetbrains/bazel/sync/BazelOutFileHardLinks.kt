package org.jetbrains.bazel.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
interface BazelOutFileHardLinks {
  fun onBeforeSync()
  suspend fun onAfterSync(projectModelUpdated: Boolean)

  /**
   * Creates a hard link to Bazel's outputs during sync (e.g., jars).
   *
   * The reason is that Bazel with `--remote_download_minimal` can garbage collect jars it thinks are "not needed"
   * from the output base (see https://youtrack.jetbrains.com/issue/BAZEL-3049), so we cannot rely on them always being there.
   * Using hard links also makes sure fsnotifier isn't spammed by file events whenever Bazel builds a target.
   *
   * NOTE: The operation is expensive, so consider async execution
   *
   * @return the created hard link, [originalFile] itself if it isn't an output file (e.g. a for source file), or `null` if [originalFile] doesn't exist
   */
  fun createOutputFileHardLink(originalFile: Path): Path?

  companion object {
    val NONE = object: BazelOutFileHardLinks {
      override fun onBeforeSync() {}
      override suspend fun onAfterSync(projectModelUpdated: Boolean) {}
      override fun createOutputFileHardLink(originalFile: Path): Path = originalFile
    }
  }
}

@ApiStatus.Internal
suspend fun BazelOutFileHardLinks.createOutputFileHardLinks(files: Collection<Path>): List<Path> =
  when (files.size) {
    0 -> emptyList()
    1 -> listOfNotNull(createOutputFileHardLink(files.first()))
    else -> coroutineScope {
      files.map { file ->
        async(Dispatchers.IO) {
          this@createOutputFileHardLinks.createOutputFileHardLink(file)
        }
      }.awaitAll().filterNotNull()
    }
  }
