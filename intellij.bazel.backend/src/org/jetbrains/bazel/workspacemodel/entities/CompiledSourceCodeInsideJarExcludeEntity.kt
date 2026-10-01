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
  /**
   * Paths of the project source files whose compiled classes and copies have to be excluded from internal jars.
   * Package of a file inside a jar is approximated by its relative path, see `CompiledSourceFileIndex`.
   */
  val relativePathsInsideJarToExclude: Set<String>
  val librariesFromInternalTargetsUrls: Set<String>

  val excludeId: CompiledSourceCodeInsideJarExcludeId
  override val symbolicId: CompiledSourceCodeInsideJarExcludeId
    get() = excludeId
}

@ApiStatus.Internal
interface LibraryCompiledSourceCodeInsideJarExcludeEntity : WorkspaceEntity {
  val libraryId: LibraryId
  val compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId
}
