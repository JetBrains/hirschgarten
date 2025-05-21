package org.jetbrains.bazel.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.commons.constants.Constants
import java.io.File
import java.io.IOException
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

internal val BUILD_FILE_GLOB = "{${Constants.BUILD_FILE_NAMES.joinToString(",")}}"

private val log = logger<VfsUtils>()

/** A helper class */
object VfsUtils {
  /**
   * Attempts to resolve the given file path to a [VirtualFile].
   *
   * @param refreshIfNeeded whether to refresh the file in the VFS, if it is not already cached.
   *                        Will only refresh if called on the EDT.
   */
  @JvmStatic
  fun resolveVirtualFile(file: File, refreshIfNeeded: Boolean): VirtualFile? {
    val localFileSystem = LocalFileSystem.getInstance()
    var vf = localFileSystem.findFileByPathIfCached(file.path)
    if (vf != null) {
      return vf
    }
    vf = localFileSystem.findFileByIoFile(file)
    if (vf != null && vf.isValid) {
      return vf
    }
    val shouldRefresh = refreshIfNeeded && ApplicationManager.getApplication().isDispatchThread
    return if (shouldRefresh) localFileSystem.refreshAndFindFileByIoFile(file) else null
  }

  fun getBuildFileForPackageDirectory(packageDirectory: VirtualFile): VirtualFile? {
    try {
      if (!packageDirectory.isDirectory) return null
      val path = packageDirectory.toNioPath()
      return path
        .listDirectoryEntries(
          glob = BUILD_FILE_GLOB,
        ).firstOrNull { it.isRegularFile() }
        ?.toVirtualFile()
    } catch (e: IOException) {
      log.warn("Cannot retrieve Bazel BUILD file from directory $this", e)
      return null
    }
  }
}
