package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.resolveFromRelativeOrRoot(absoluteOrRelativeFilePath: String): VirtualFile? {
  return findFileByRelativePath(absoluteOrRelativeFilePath) ?: fileSystem.findFileByPath(absoluteOrRelativeFilePath)
}
