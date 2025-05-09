package org.jetbrains.bazel.workspacemodel.entities

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

  public val excludeId: CompiledSourceCodeInsideJarExcludeId
  override val symbolicId: CompiledSourceCodeInsideJarExcludeId
    get() = excludeId

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<CompiledSourceCodeInsideJarExcludeEntity> {
    override var entitySource: EntitySource
    var relativePathsInsideJarToExclude: MutableSet<String>
    var excludeId: CompiledSourceCodeInsideJarExcludeId
  }

  companion object : EntityType<CompiledSourceCodeInsideJarExcludeEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      relativePathsInsideJarToExclude: Set<String>,
      excludeId: CompiledSourceCodeInsideJarExcludeId,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.relativePathsInsideJarToExclude = relativePathsInsideJarToExclude.toMutableWorkspaceSet()
      builder.excludeId = excludeId
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyCompiledSourceCodeInsideJarExcludeEntity(
  entity: CompiledSourceCodeInsideJarExcludeEntity,
  modification: CompiledSourceCodeInsideJarExcludeEntity.Builder.() -> Unit,
): CompiledSourceCodeInsideJarExcludeEntity =
  modifyEntity(CompiledSourceCodeInsideJarExcludeEntity.Builder::class.java, entity, modification)
//endregion

interface LibraryCompiledSourceCodeInsideJarExcludeEntity : WorkspaceEntity {
  val libraryId: LibraryId
  val compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<LibraryCompiledSourceCodeInsideJarExcludeEntity> {
    override var entitySource: EntitySource
    var libraryId: LibraryId
    var compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId
  }

  companion object : EntityType<LibraryCompiledSourceCodeInsideJarExcludeEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      libraryId: LibraryId,
      compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.libraryId = libraryId
      builder.compiledSourceCodeInsideJarExcludeId = compiledSourceCodeInsideJarExcludeId
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyLibraryCompiledSourceCodeInsideJarExcludeEntity(
  entity: LibraryCompiledSourceCodeInsideJarExcludeEntity,
  modification: LibraryCompiledSourceCodeInsideJarExcludeEntity.Builder.() -> Unit,
): LibraryCompiledSourceCodeInsideJarExcludeEntity =
  modifyEntity(LibraryCompiledSourceCodeInsideJarExcludeEntity.Builder::class.java, entity, modification)
//endregion
