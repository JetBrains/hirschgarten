package org.jetbrains.bazel.test.framework

import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import java.nio.file.Path
import kotlin.io.path.Path

class RecordingBazelOutFileHardLinks(private val cacheRoot: Path = Path("cached")) : BazelOutFileHardLinks {
  val linkedFiles: MutableList<Path> = mutableListOf()

  override fun onBeforeSync() {}

  override suspend fun onAfterSync(fullProjectModelUpdated: Boolean) {}

  override suspend fun createOutputFileHardLinks(files: Collection<Path>): List<Path> {
    linkedFiles.addAll(files)
    return files.toList()
  }

  override suspend fun createOutputFileHardLink(originalFile: Path): Path {
    linkedFiles.add(originalFile)
    return originalFile
  }

  override fun resolveCachedPath(fileOrDir: Path): Path = cacheRoot.resolve(fileOrDir.root?.relativize(fileOrDir) ?: fileOrDir)

  override val allHardLinksCreatedSuccessfully: Boolean = true
}
