package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.ModifiableWorkspaceEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceSet

@GeneratedCodeApiVersion(3)
interface ModifiableLibraryCompiledSourceCodeInsideJarExcludeEntity : ModifiableWorkspaceEntity<LibraryCompiledSourceCodeInsideJarExcludeEntity> {
  override var entitySource: EntitySource
  var libraryId: LibraryId
  var compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId
}

internal object LibraryCompiledSourceCodeInsideJarExcludeEntityType : EntityType<LibraryCompiledSourceCodeInsideJarExcludeEntity, ModifiableLibraryCompiledSourceCodeInsideJarExcludeEntity>() {
  override val entityClass: Class<LibraryCompiledSourceCodeInsideJarExcludeEntity> get() = LibraryCompiledSourceCodeInsideJarExcludeEntity::class.java
  operator fun invoke(
    libraryId: LibraryId,
    compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId,
    entitySource: EntitySource,
    init: (ModifiableLibraryCompiledSourceCodeInsideJarExcludeEntity.() -> Unit)? = null,
  ): ModifiableLibraryCompiledSourceCodeInsideJarExcludeEntity {
    val builder = builder()
    builder.libraryId = libraryId
    builder.compiledSourceCodeInsideJarExcludeId = compiledSourceCodeInsideJarExcludeId
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

fun MutableEntityStorage.modifyLibraryCompiledSourceCodeInsideJarExcludeEntity(
  entity: LibraryCompiledSourceCodeInsideJarExcludeEntity,
  modification: ModifiableLibraryCompiledSourceCodeInsideJarExcludeEntity.() -> Unit,
): LibraryCompiledSourceCodeInsideJarExcludeEntity =
  modifyEntity(ModifiableLibraryCompiledSourceCodeInsideJarExcludeEntity::class.java, entity, modification)

@JvmOverloads
@JvmName("createLibraryCompiledSourceCodeInsideJarExcludeEntity")
fun LibraryCompiledSourceCodeInsideJarExcludeEntity(
  libraryId: LibraryId,
  compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId,
  entitySource: EntitySource,
  init: (ModifiableLibraryCompiledSourceCodeInsideJarExcludeEntity.() -> Unit)? = null,
): ModifiableLibraryCompiledSourceCodeInsideJarExcludeEntity =
  LibraryCompiledSourceCodeInsideJarExcludeEntityType(libraryId, compiledSourceCodeInsideJarExcludeId, entitySource, init)
