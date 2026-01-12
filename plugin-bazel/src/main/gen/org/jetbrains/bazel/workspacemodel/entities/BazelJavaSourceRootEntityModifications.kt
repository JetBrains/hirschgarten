@file:JvmName("BazelJavaSourceRootEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface BazelJavaSourceRootEntityBuilder : WorkspaceEntityBuilder<BazelJavaSourceRootEntity> {
  override var entitySource: EntitySource
  var packageNameId: PackageNameId
  var sourceRoots: MutableList<VirtualFileUrl>
}

internal object BazelJavaSourceRootEntityType : EntityType<BazelJavaSourceRootEntity, BazelJavaSourceRootEntityBuilder>() {
  override val entityClass: Class<BazelJavaSourceRootEntity> get() = BazelJavaSourceRootEntity::class.java
  operator fun invoke(
    packageNameId: PackageNameId,
    sourceRoots: List<VirtualFileUrl>,
    entitySource: EntitySource,
    init: (BazelJavaSourceRootEntityBuilder.() -> Unit)? = null,
  ): BazelJavaSourceRootEntityBuilder {
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
  modification: BazelJavaSourceRootEntityBuilder.() -> Unit,
): BazelJavaSourceRootEntity = modifyEntity(BazelJavaSourceRootEntityBuilder::class.java, entity, modification)

@JvmOverloads
@JvmName("createBazelJavaSourceRootEntity")
fun BazelJavaSourceRootEntity(
  packageNameId: PackageNameId,
  sourceRoots: List<VirtualFileUrl>,
  entitySource: EntitySource,
  init: (BazelJavaSourceRootEntityBuilder.() -> Unit)? = null,
): BazelJavaSourceRootEntityBuilder = BazelJavaSourceRootEntityType(packageNameId, sourceRoots, entitySource, init)
