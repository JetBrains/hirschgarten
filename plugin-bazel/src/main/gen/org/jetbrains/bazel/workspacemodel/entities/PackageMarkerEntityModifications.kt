@file:JvmName("PackageMarkerEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface PackageMarkerEntityBuilder : WorkspaceEntityBuilder<PackageMarkerEntity> {
  override var entitySource: EntitySource
  var root: VirtualFileUrl
  var packagePrefix: String
  var module: ModuleEntityBuilder
}

internal object PackageMarkerEntityType : EntityType<PackageMarkerEntity, PackageMarkerEntityBuilder>() {
  override val entityClass: Class<PackageMarkerEntity> get() = PackageMarkerEntity::class.java
  operator fun invoke(
    root: VirtualFileUrl,
    packagePrefix: String,
    entitySource: EntitySource,
    init: (PackageMarkerEntityBuilder.() -> Unit)? = null,
  ): PackageMarkerEntityBuilder {
    val builder = builder()
    builder.root = root
    builder.packagePrefix = packagePrefix
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

fun MutableEntityStorage.modifyPackageMarkerEntity(
  entity: PackageMarkerEntity,
  modification: PackageMarkerEntityBuilder.() -> Unit,
): PackageMarkerEntity = modifyEntity(PackageMarkerEntityBuilder::class.java, entity, modification)

var ModuleEntityBuilder.packageMarkerEntities: List<PackageMarkerEntityBuilder>
  by WorkspaceEntity.extensionBuilder(PackageMarkerEntity::class.java)


@JvmOverloads
@JvmName("createPackageMarkerEntity")
fun PackageMarkerEntity(
  root: VirtualFileUrl,
  packagePrefix: String,
  entitySource: EntitySource,
  init: (PackageMarkerEntityBuilder.() -> Unit)? = null,
): PackageMarkerEntityBuilder = PackageMarkerEntityType(root, packagePrefix, entitySource, init)
