package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

data class PackageNameId(val packageName: String) : SymbolicEntityId<WorkspaceEntityWithSymbolicId> {
  override val presentableName: String
    get() = packageName
}

interface BazelJavaSourceRootEntity : WorkspaceEntity {
  val packageNameId: PackageNameId
  val sourceRoots: List<VirtualFileUrl>
}
