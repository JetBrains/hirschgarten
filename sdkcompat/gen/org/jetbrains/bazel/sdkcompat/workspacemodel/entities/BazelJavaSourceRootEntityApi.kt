package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.ModifiableWorkspaceEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface ModifiableBazelJavaSourceRootEntity : ModifiableWorkspaceEntity<BazelJavaSourceRootEntity> {
  override var entitySource: EntitySource
  var packageNameId: PackageNameId
  var sourceRoots: MutableList<VirtualFileUrl>
}

internal object BazelJavaSourceRootEntityType : EntityType<BazelJavaSourceRootEntity, ModifiableBazelJavaSourceRootEntity>() {
  override val entityClass: Class<BazelJavaSourceRootEntity> get() = BazelJavaSourceRootEntity::class.java
  operator fun invoke(
    packageNameId: PackageNameId,
    sourceRoots: List<VirtualFileUrl>,
    entitySource: EntitySource,
    init: (ModifiableBazelJavaSourceRootEntity.() -> Unit)? = null,
  ): ModifiableBazelJavaSourceRootEntity {
    val builder = builder()
    builder.packageNameId = packageNameId
    builder.sourceRoots = sourceRoots.toMutableWorkspaceList()
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

fun MutableEntityStorage.modifyBazelJavaSourceRootEntity(
  entity: BazelJavaSourceRootEntity,
  modification: ModifiableBazelJavaSourceRootEntity.() -> Unit,
): BazelJavaSourceRootEntity = modifyEntity(ModifiableBazelJavaSourceRootEntity::class.java, entity, modification)

@JvmOverloads
@JvmName("createBazelJavaSourceRootEntity")
fun BazelJavaSourceRootEntity(
  packageNameId: PackageNameId,
  sourceRoots: List<VirtualFileUrl>,
  entitySource: EntitySource,
  init: (ModifiableBazelJavaSourceRootEntity.() -> Unit)? = null,
): ModifiableBazelJavaSourceRootEntity = BazelJavaSourceRootEntityType(packageNameId, sourceRoots, entitySource, init)
