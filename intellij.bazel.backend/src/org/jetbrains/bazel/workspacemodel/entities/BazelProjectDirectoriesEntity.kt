package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.ApiStatus

// wrapper to avoid `VirtualFileUrl` indexing,
//  workspace model only index top level properties
@ApiStatus.Internal
data class NonIndexableVirtualFileUrl(val url: VirtualFileUrl)

@ApiStatus.Internal
interface BazelProjectDirectoriesEntity : WorkspaceEntity {
  public val projectRoot: VirtualFileUrl
  public val includedRoots: List<NonIndexableVirtualFileUrl>
  public val excludedRoots: List<NonIndexableVirtualFileUrl>
  public val indexAllFilesInIncludedRoots: Boolean
  public val indexAdditionalFiles: List<NonIndexableVirtualFileUrl>
}
