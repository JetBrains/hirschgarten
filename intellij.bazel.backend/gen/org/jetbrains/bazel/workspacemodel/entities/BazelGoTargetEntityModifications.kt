@file:JvmName("BazelGoTargetEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.workspacemodel.entities.impl.BazelGoTargetEntityImpl

@Internal
@GeneratedCodeApiVersion(3)
interface BazelGoTargetEntityBuilder : WorkspaceEntityBuilder<BazelGoTargetEntity> {
  override var entitySource: EntitySource
  var _targetKey: WorkspaceModelTargetKey
  var importPath: ImportPathId
}

internal object BazelGoTargetEntityType : EntityType<BazelGoTargetEntity, BazelGoTargetEntityBuilder>() {
  override val entityClass: Class<BazelGoTargetEntity> get() = BazelGoTargetEntity::class.java
  override val entityImplBuilderClass: Class<*> get() = BazelGoTargetEntityImpl.Builder::class.java
  operator fun invoke(
    _targetKey: WorkspaceModelTargetKey,
    importPath: ImportPathId,
    entitySource: EntitySource,
    init: (BazelGoTargetEntityBuilder.() -> Unit)? = null,
  ): BazelGoTargetEntityBuilder {
    val builder = builder()
    builder._targetKey = _targetKey
    builder.importPath = importPath
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

@Internal
fun MutableEntityStorage.modifyBazelGoTargetEntity(
  entity: BazelGoTargetEntity,
  modification: BazelGoTargetEntityBuilder.() -> Unit,
): BazelGoTargetEntity = modifyEntity(BazelGoTargetEntityBuilder::class.java, entity, modification)

@Internal
@JvmOverloads
@JvmName("createBazelGoTargetEntity")
fun BazelGoTargetEntity(
  _targetKey: WorkspaceModelTargetKey,
  importPath: ImportPathId,
  entitySource: EntitySource,
  init: (BazelGoTargetEntityBuilder.() -> Unit)? = null,
): BazelGoTargetEntityBuilder = BazelGoTargetEntityType(_targetKey, importPath, entitySource, init)
