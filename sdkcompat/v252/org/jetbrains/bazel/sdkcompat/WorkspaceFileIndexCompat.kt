package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndex

fun WorkspaceFileIndex.isIndexableCompat(file: VirtualFile): Boolean =
  findFileSet(
    file,
    true,
    true,
    true,
    true,
    true,
  ) != null
