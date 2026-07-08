@file:JvmName("BazelGoPackageEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.workspacemodel.entities.impl.BazelGoPackageEntityImpl

@Internal
@GeneratedCodeApiVersion(3)
interface BazelGoPackageEntityBuilder : WorkspaceEntityBuilder<BazelGoPackageEntity> {
  override var entitySource: EntitySource
  var importPath: String
  var sources: MutableList<VirtualFileUrl>
}

internal object BazelGoPackageEntityType : EntityType<BazelGoPackageEntity, BazelGoPackageEntityBuilder>() {
  override val entityClass: Class<BazelGoPackageEntity> get() = BazelGoPackageEntity::class.java
  override val entityImplBuilderClass: Class<*> get() = BazelGoPackageEntityImpl.Builder::class.java
  operator fun invoke(
    importPath: String,
    sources: List<VirtualFileUrl>,
    entitySource: EntitySource,
    init: (BazelGoPackageEntityBuilder.() -> Unit)? = null,
  ): BazelGoPackageEntityBuilder {
    val builder = builder()
    builder.importPath = importPath
    builder.sources = sources.toMutableWorkspaceList()
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

@Internal
fun MutableEntityStorage.modifyBazelGoPackageEntity(
  entity: BazelGoPackageEntity,
  modification: BazelGoPackageEntityBuilder.() -> Unit,
): BazelGoPackageEntity = modifyEntity(BazelGoPackageEntityBuilder::class.java, entity, modification)

@Internal
@JvmOverloads
@JvmName("createBazelGoPackageEntity")
fun BazelGoPackageEntity(
  importPath: String,
  sources: List<VirtualFileUrl>,
  entitySource: EntitySource,
  init: (BazelGoPackageEntityBuilder.() -> Unit)? = null,
): BazelGoPackageEntityBuilder = BazelGoPackageEntityType(importPath, sources, entitySource, init)
