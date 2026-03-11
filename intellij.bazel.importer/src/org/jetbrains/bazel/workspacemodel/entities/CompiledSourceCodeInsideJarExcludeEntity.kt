package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class CompiledSourceCodeInsideJarExcludeId(val id: Int) : SymbolicEntityId<CompiledSourceCodeInsideJarExcludeEntity> {
  override val presentableName: String
    get() = toString()
}

@ApiStatus.Internal
interface CompiledSourceCodeInsideJarExcludeEntity : WorkspaceEntityWithSymbolicId {
  public val relativePathsInsideJarToExclude: Set<String>
  public val librariesFromInternalTargetsUrls: Set<String>

  public val excludeId: CompiledSourceCodeInsideJarExcludeId
  override val symbolicId: CompiledSourceCodeInsideJarExcludeId
    get() = excludeId
}

@ApiStatus.Internal
interface LibraryCompiledSourceCodeInsideJarExcludeEntity : WorkspaceEntity {
  val libraryId: LibraryId
  val compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId
}
