package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/** A helper class
 * see com.google.idea.blaze.base.io.VfsUtils
 * */
object VfsUtils {
  /**
   * Attempts to resolve the given file path to a [VirtualFile].
   *
   *
   * WARNING: Refreshing files on the EDT may freeze the IDE.
   *
   * @param refreshIfNeeded whether to refresh the file in the VFS, if it is not already cached.
   * Will only refresh if called on the EDT.
   */
  fun resolveVirtualFile(file: File, refreshIfNeeded: Boolean): VirtualFile? {
    val fileSystem = LocalFileSystem.getInstance()
    var vf = fileSystem.findFileByPathIfCached(file.path)
    if (vf != null) {
      return vf
    }
    vf = fileSystem.findFileByIoFile(file)
    if (vf != null && vf.isValid) {
      return vf
    }
    val shouldRefresh =
      refreshIfNeeded && ApplicationManager.getApplication().isDispatchThread
    return if (shouldRefresh) fileSystem.refreshAndFindFileByIoFile(file) else null
  }
}
