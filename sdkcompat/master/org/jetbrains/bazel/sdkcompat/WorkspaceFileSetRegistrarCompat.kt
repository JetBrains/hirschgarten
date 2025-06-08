package org.jetbrains.bazel.sdkcompat

import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar

fun WorkspaceFileSetRegistrar.registerOtherRootsCompat(
  projectRoot: VirtualFileUrl,
  includedRoots: List<VirtualFileUrl>,
  entity: WorkspaceEntity,
) {
  registerFileSet(
    root = projectRoot,
    kind = WorkspaceFileKind.CONTENT_NON_INDEXABLE,
    entity = entity,
    customData = null,
  )
}
