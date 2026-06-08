package org.jetbrains.bazel.sync

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
  suspend fun createOutputFileHardLink(originalFile: Path): Path? =
    createOutputFileHardLinks(listOf(originalFile)).firstOrNull()

  /**
   * Creates hard links to Bazel's outputs during sync (e.g., jars) for a batch of files.
   *
   * The reason is that Bazel with `--remote_download_minimal` can garbage collect jars it thinks are "not needed"
   * from the output base (see https://youtrack.jetbrains.com/issue/BAZEL-3049), so we cannot rely on them always being there.
   * Using hard links also makes sure fsnotifier isn't spammed by file events whenever Bazel builds a target.
   *
   * This is the batched entry point; [createOutputFileHardLink] delegates to it for single-file callers.
   *
   * NOTE: The operation is expensive, so consider async execution
   *
   * @param files the original paths to link.
   * @return a list of resulting paths, where each entry is either the created hard link, the [Path] itself if it isn't an
   * output file (e.g. a source file), or omitted if the corresponding original file doesn't exist. The size and order of
   * the result are not guaranteed to match [files].
   */
  suspend fun createOutputFileHardLinks(files: Collection<Path>): List<Path>

  val allHardLinksCreatedSuccessfully: Boolean

  companion object {
    val NONE = object: BazelOutFileHardLinks {
      override fun onBeforeSync() {}
      override suspend fun onAfterSync(projectModelUpdated: Boolean) {}
      override suspend fun createOutputFileHardLink(originalFile: Path): Path = originalFile
      override suspend fun createOutputFileHardLinks(files: Collection<Path>): List<Path> = files.toList()

      override val allHardLinksCreatedSuccessfully: Boolean
        get() = false
    }
  }
}
