package org.jetbrains.bazel.workspace

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetExclusionCondition
import com.intellij.workspaceModel.ide.toPath
import java.nio.file.Path

internal class ExcludeAllNonExplicitlyIncludedFiles private constructor(
  private val includedRoots: Set<Path>,
  private val ancestors: Set<Path>
) : WorkspaceFileSetExclusionCondition {
  override fun shouldExclude(file: VirtualFile): Boolean {
    if (!file.isDirectory) return false
    val filePath = file.toNioPathOrNull() ?: return false
    if (filePath in ancestors) return false
    return includedRoots.none { filePath.startsWith(it) }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ExcludeAllNonExplicitlyIncludedFiles

    if (includedRoots != other.includedRoots) return false
    if (ancestors != other.ancestors) return false

    return true
  }

  override fun hashCode(): Int {
    var result = includedRoots.hashCode()
    result = 31 * result + ancestors.hashCode()
    return result
  }

  companion object {
    fun createExcludeAllNonExplicitlyIncludedFiles(includedRoots: List<VirtualFileUrl>): ExcludeAllNonExplicitlyIncludedFiles {
      val includedRoots: Set<Path> = includedRoots.mapTo(hashSetOf()) { it.toPath() }
      val ancestors: Set<Path> = includedRoots.flatMapTo(hashSetOf()) { findAllParents(it) }
      return ExcludeAllNonExplicitlyIncludedFiles(includedRoots, ancestors)
    }

    private fun findAllParents(path: Path): Set<Path> {
      val parents = hashSetOf<Path>()
      var currentPath = path.parent
      while (currentPath != null) {
        parents.add(currentPath)
        currentPath = currentPath.parent
      }
      return parents
    }
  }
}
