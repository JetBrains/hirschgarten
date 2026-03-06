package org.jetbrains.bazel.workspace

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetExclusionCondition
import com.intellij.workspaceModel.ide.toPath
import java.nio.file.Path

internal class ExcludeAllNonExplicitlyIncludedFiles : WorkspaceFileSetExclusionCondition {
  private val includedRoots: Set<Path>

  constructor(includedRoots: List<VirtualFileUrl>) {
    this.includedRoots = includedRoots.map { it.toPath() }.toSet()
  }

  override fun shouldExclude(file: VirtualFile): Boolean {
    val filePath = file.toNioPathOrNull() ?: return false
    return includedRoots.none { filePath.startsWith(it) }
  }

  override fun equals(other: Any?): Boolean {
    if (other !is ExcludeAllNonExplicitlyIncludedFiles) return false
    return other.includedRoots == this.includedRoots
  }

  override fun hashCode(): Int = includedRoots.hashCode()
}
