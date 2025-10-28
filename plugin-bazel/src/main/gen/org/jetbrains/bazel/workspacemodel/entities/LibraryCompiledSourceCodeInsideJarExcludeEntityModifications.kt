@file:JvmName("LibraryCompiledSourceCodeInsideJarExcludeEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId

@GeneratedCodeApiVersion(3)
interface LibraryCompiledSourceCodeInsideJarExcludeEntityBuilder : WorkspaceEntityBuilder<LibraryCompiledSourceCodeInsideJarExcludeEntity> {
  override var entitySource: EntitySource
  var libraryId: LibraryId
  var compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId
}

internal object LibraryCompiledSourceCodeInsideJarExcludeEntityType : EntityType<LibraryCompiledSourceCodeInsideJarExcludeEntity, LibraryCompiledSourceCodeInsideJarExcludeEntityBuilder>() {
  override val entityClass: Class<LibraryCompiledSourceCodeInsideJarExcludeEntity> get() = LibraryCompiledSourceCodeInsideJarExcludeEntity::class.java
  operator fun invoke(
    libraryId: LibraryId,
    compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId,
    entitySource: EntitySource,
    init: (LibraryCompiledSourceCodeInsideJarExcludeEntityBuilder.() -> Unit)? = null,
  ): LibraryCompiledSourceCodeInsideJarExcludeEntityBuilder {
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
  modification: LibraryCompiledSourceCodeInsideJarExcludeEntityBuilder.() -> Unit,
): LibraryCompiledSourceCodeInsideJarExcludeEntity =
  modifyEntity(LibraryCompiledSourceCodeInsideJarExcludeEntityBuilder::class.java, entity, modification)

@JvmOverloads
@JvmName("createLibraryCompiledSourceCodeInsideJarExcludeEntity")
fun LibraryCompiledSourceCodeInsideJarExcludeEntity(
  libraryId: LibraryId,
  compiledSourceCodeInsideJarExcludeId: CompiledSourceCodeInsideJarExcludeId,
  entitySource: EntitySource,
  init: (LibraryCompiledSourceCodeInsideJarExcludeEntityBuilder.() -> Unit)? = null,
): LibraryCompiledSourceCodeInsideJarExcludeEntityBuilder =
  LibraryCompiledSourceCodeInsideJarExcludeEntityType(libraryId, compiledSourceCodeInsideJarExcludeId, entitySource, init)
