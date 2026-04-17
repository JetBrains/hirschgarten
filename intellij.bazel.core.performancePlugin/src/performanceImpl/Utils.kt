package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
fun VirtualFile.resolveFromRelativeOrRoot(absoluteOrRelativeFilePath: String): VirtualFile? {
  return findFileByRelativePath(absoluteOrRelativeFilePath) ?: fileSystem.findFileByPath(absoluteOrRelativeFilePath)
}
