package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

interface BazelProjectDirectoriesEntity : WorkspaceEntity {
  public val projectRoot: VirtualFileUrl
  public val includedRoots: List<VirtualFileUrl>
  public val excludedRoots: List<VirtualFileUrl>
  public val indexAllFilesInIncludedRoots: Boolean
  public val indexAdditionalFiles: List<VirtualFileUrl>
}
