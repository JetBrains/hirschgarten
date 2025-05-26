package org.jetbrains.bazel.utils

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.commons.constants.Constants
import java.io.IOException
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

internal val BUILD_FILE_GLOB = "{${Constants.BUILD_FILE_NAMES.joinToString(",")}}"

private val log = logger<VfsUtils>()

object VfsUtils {
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
