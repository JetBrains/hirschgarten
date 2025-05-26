package org.jetbrains.bazel.sdkcompat

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetData
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar

fun WorkspaceFileSetRegistrar.registerFileSetByConditionCompat(
  root: VirtualFileUrl,
  kind: WorkspaceFileKind,
  entity: WorkspaceEntity,
  customData: WorkspaceFileSetData?,
  condition: (VirtualFile) -> Boolean,
): Unit = throw UnsupportedOperationException()
