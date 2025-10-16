package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.AbstractBazelProjectDirectoriesEntity

interface BazelProjectDirectoriesEntity : AbstractBazelProjectDirectoriesEntity {
  public val includedRoots: List<VirtualFileUrl>
  public val excludedRoots: List<VirtualFileUrl>
  public val indexAllFilesInIncludedRoots: Boolean
  public val indexAdditionalFiles: List<VirtualFileUrl>
}
