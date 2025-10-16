package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceSet

data class CompiledSourceCodeInsideJarExcludeId(val id: Int) : SymbolicEntityId<CompiledSourceCodeInsideJarExcludeEntity> {
  override val presentableName: String
    get() = toString()
}

interface CompiledSourceCodeInsideJarExcludeEntity : WorkspaceEntityWithSymbolicId {
  public val relativePathsInsideJarToExclude: Set<String>
  public val librariesFromInternalTargetsUrls: Set<String>

  public val excludeId: CompiledSourceCodeInsideJarExcludeId
  override val symbolicId: CompiledSourceCodeInsideJarExcludeId
    get() = excludeId
}

interface LibraryCompiledSourceCodeInsideJarExcludeEntity : WorkspaceEntity {
  val libraryId: LibraryId
  val compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId
}
