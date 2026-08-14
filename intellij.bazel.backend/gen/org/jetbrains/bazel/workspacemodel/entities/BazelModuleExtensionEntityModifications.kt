@file:JvmName("BazelModuleExtensionEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.workspacemodel.entities.impl.BazelModuleExtensionEntityImpl

@Internal
@GeneratedCodeApiVersion(3)
interface BazelModuleExtensionEntityBuilder : WorkspaceEntityBuilder<BazelModuleExtensionEntity> {
  override var entitySource: EntitySource
  var module: ModuleEntityBuilder
  var _targetKey: WorkspaceModelTargetKey
  var rootTypeId: WorkspaceModelTargetSourceRootTypeId
  var strictDependencies: WorkspaceModelTargetLabelList
}

internal object BazelModuleExtensionEntityType : EntityType<BazelModuleExtensionEntity, BazelModuleExtensionEntityBuilder>() {
  override val entityClass: Class<BazelModuleExtensionEntity> get() = BazelModuleExtensionEntity::class.java
  override val entityImplBuilderClass: Class<*> get() = BazelModuleExtensionEntityImpl.Builder::class.java
  operator fun invoke(
    _targetKey: WorkspaceModelTargetKey,
    rootTypeId: WorkspaceModelTargetSourceRootTypeId,
    strictDependencies: WorkspaceModelTargetLabelList,
    entitySource: EntitySource,
    init: (BazelModuleExtensionEntityBuilder.() -> Unit)? = null,
  ): BazelModuleExtensionEntityBuilder {
    val builder = builder()
    builder._targetKey = _targetKey
    builder.rootTypeId = rootTypeId
    builder.strictDependencies = strictDependencies
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

@Internal
fun MutableEntityStorage.modifyBazelModuleExtensionEntity(
  entity: BazelModuleExtensionEntity,
  modification: BazelModuleExtensionEntityBuilder.() -> Unit,
): BazelModuleExtensionEntity = modifyEntity(BazelModuleExtensionEntityBuilder::class.java, entity, modification)

@get:Internal
@set:Internal
var ModuleEntityBuilder.bazelModuleExtension: BazelModuleExtensionEntityBuilder?
  by WorkspaceEntity.extensionBuilder(BazelModuleExtensionEntity::class.java)


@Internal
@JvmOverloads
@JvmName("createBazelModuleExtensionEntity")
fun BazelModuleExtensionEntity(
  _targetKey: WorkspaceModelTargetKey,
  rootTypeId: WorkspaceModelTargetSourceRootTypeId,
  strictDependencies: WorkspaceModelTargetLabelList,
  entitySource: EntitySource,
  init: (BazelModuleExtensionEntityBuilder.() -> Unit)? = null,
): BazelModuleExtensionEntityBuilder = BazelModuleExtensionEntityType(_targetKey, rootTypeId, strictDependencies, entitySource, init)
