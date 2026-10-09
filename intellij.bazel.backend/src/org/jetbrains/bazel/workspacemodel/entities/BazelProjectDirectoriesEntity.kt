package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.IndexVfu
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface BazelProjectDirectoriesEntity : WorkspaceEntity {
  @IndexVfu
  public val projectRoot: VirtualFileUrl
  public val includedRoots: List<VirtualFileUrl>
  public val excludedRoots: List<VirtualFileUrl>

  /**
   * Patterns used to compute [indexableRecursiveRoots] and [indexableNonRecursiveRoots].
   * We need to preserve them to properly process file events and keep [indexableNonRecursiveRoots] up to date.
   */
  public val indexPatterns: List<String>
  public val indexableRecursiveRoots: List<VirtualFileUrl>
  public val indexableNonRecursiveRoots: List<VirtualFileUrl>
}
