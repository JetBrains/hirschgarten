@file:JvmName("CompiledSourceCodeInsideJarExcludeEntityModifications")

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
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceSet

@GeneratedCodeApiVersion(3)
interface CompiledSourceCodeInsideJarExcludeEntityBuilder : WorkspaceEntityBuilder<CompiledSourceCodeInsideJarExcludeEntity> {
  override var entitySource: EntitySource
  var relativePathsInsideJarToExclude: MutableSet<String>
  var librariesFromInternalTargetsUrls: MutableSet<String>
  var excludeId: CompiledSourceCodeInsideJarExcludeId
}

internal object CompiledSourceCodeInsideJarExcludeEntityType : EntityType<CompiledSourceCodeInsideJarExcludeEntity, CompiledSourceCodeInsideJarExcludeEntityBuilder>() {
  override val entityClass: Class<CompiledSourceCodeInsideJarExcludeEntity> get() = CompiledSourceCodeInsideJarExcludeEntity::class.java
  operator fun invoke(
    relativePathsInsideJarToExclude: Set<String>,
    librariesFromInternalTargetsUrls: Set<String>,
    excludeId: CompiledSourceCodeInsideJarExcludeId,
    entitySource: EntitySource,
    init: (CompiledSourceCodeInsideJarExcludeEntityBuilder.() -> Unit)? = null,
  ): CompiledSourceCodeInsideJarExcludeEntityBuilder {
    val builder = builder()
    builder.relativePathsInsideJarToExclude = relativePathsInsideJarToExclude.toMutableWorkspaceSet()
    builder.librariesFromInternalTargetsUrls = librariesFromInternalTargetsUrls.toMutableWorkspaceSet()
    builder.excludeId = excludeId
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

fun MutableEntityStorage.modifyCompiledSourceCodeInsideJarExcludeEntity(
  entity: CompiledSourceCodeInsideJarExcludeEntity,
  modification: CompiledSourceCodeInsideJarExcludeEntityBuilder.() -> Unit,
): CompiledSourceCodeInsideJarExcludeEntity =
  modifyEntity(CompiledSourceCodeInsideJarExcludeEntityBuilder::class.java, entity, modification)

@JvmOverloads
@JvmName("createCompiledSourceCodeInsideJarExcludeEntity")
fun CompiledSourceCodeInsideJarExcludeEntity(
  relativePathsInsideJarToExclude: Set<String>,
  librariesFromInternalTargetsUrls: Set<String>,
  excludeId: CompiledSourceCodeInsideJarExcludeId,
  entitySource: EntitySource,
  init: (CompiledSourceCodeInsideJarExcludeEntityBuilder.() -> Unit)? = null,
): CompiledSourceCodeInsideJarExcludeEntityBuilder =
  CompiledSourceCodeInsideJarExcludeEntityType(relativePathsInsideJarToExclude, librariesFromInternalTargetsUrls, excludeId, entitySource,
                                               init)
