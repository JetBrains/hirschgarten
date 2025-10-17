package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

public data class PackageNameId(public val packageName: String) : SymbolicEntityId<WorkspaceEntityWithSymbolicId> {
  override val presentableName: String
    get() = packageName
}

public interface BazelJavaSourceRootEntity : WorkspaceEntity {
  public val packageNameId: PackageNameId
  public val sourceRoots: List<VirtualFileUrl>
}
