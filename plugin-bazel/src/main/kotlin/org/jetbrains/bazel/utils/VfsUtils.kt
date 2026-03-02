package org.jetbrains.bazel.utils

import com.intellij.openapi.vfs.VirtualFile

internal fun VirtualFile.findNearestParent(parents: Set<VirtualFile>): VirtualFile? {
  var parent = this
  while (true) {
    if (parent in parents) return parent
    parent = parent.parent ?: return null
  }
}
