package org.jetbrains.bazel.test.framework

import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.relativeTo

class CopyingBazelOutHardLinks(private val outputBase: Path, private val hardLinksRoot: Path) : BazelOutFileHardLinks {
  val linkedFiles: MutableList<Path> = mutableListOf()

  override fun onBeforeSync() {}

  override suspend fun onAfterSync(fullProjectModelUpdated: Boolean) {}

  override suspend fun createOutputFileHardLinks(files: Collection<Path>): List<Path> =
    files.filter { it.exists() }.map { file ->
      val link = resolveCachedPath(file)
      link.createParentDirectories()
      file.copyTo(link, overwrite = true)
      linkedFiles.add(link)
      link
    }

  override fun resolveCachedPath(fileOrDir: Path): Path = hardLinksRoot.resolve(fileOrDir.relativeTo(outputBase))

  override val allHardLinksCreatedSuccessfully: Boolean = true
}
