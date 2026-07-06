@file:JvmName("BazelLibraryExtensionEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.LibraryEntityBuilder
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.workspacemodel.entities.impl.BazelLibraryExtensionEntityImpl

@Internal
@GeneratedCodeApiVersion(3)
interface BazelLibraryExtensionEntityBuilder : WorkspaceEntityBuilder<BazelLibraryExtensionEntity> {
  override var entitySource: EntitySource
  var library: LibraryEntityBuilder
  var _targetKey: WorkspaceModelTargetKey
  var isSynthetic: Boolean
}

internal object BazelLibraryExtensionEntityType : EntityType<BazelLibraryExtensionEntity, BazelLibraryExtensionEntityBuilder>() {
  override val entityClass: Class<BazelLibraryExtensionEntity> get() = BazelLibraryExtensionEntity::class.java
  override val entityImplBuilderClass: Class<*> get() = BazelLibraryExtensionEntityImpl.Builder::class.java
  operator fun invoke(
    _targetKey: WorkspaceModelTargetKey,
    isSynthetic: Boolean,
    entitySource: EntitySource,
    init: (BazelLibraryExtensionEntityBuilder.() -> Unit)? = null,
  ): BazelLibraryExtensionEntityBuilder {
    val builder = builder()
    builder._targetKey = _targetKey
    builder.isSynthetic = isSynthetic
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

@Internal
fun MutableEntityStorage.modifyBazelLibraryExtensionEntity(
  entity: BazelLibraryExtensionEntity,
  modification: BazelLibraryExtensionEntityBuilder.() -> Unit,
): BazelLibraryExtensionEntity = modifyEntity(BazelLibraryExtensionEntityBuilder::class.java, entity, modification)

@get:Internal
@set:Internal
var LibraryEntityBuilder.bazelLibraryExtension: BazelLibraryExtensionEntityBuilder?
  by WorkspaceEntity.extensionBuilder(BazelLibraryExtensionEntity::class.java)


@Internal
@JvmOverloads
@JvmName("createBazelLibraryExtensionEntity")
fun BazelLibraryExtensionEntity(
  _targetKey: WorkspaceModelTargetKey,
  isSynthetic: Boolean,
  entitySource: EntitySource,
  init: (BazelLibraryExtensionEntityBuilder.() -> Unit)? = null,
): BazelLibraryExtensionEntityBuilder = BazelLibraryExtensionEntityType(_targetKey, isSynthetic, entitySource, init)
