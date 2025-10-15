package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModifiableModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.ModifiableWorkspaceEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface ModifiablePackageMarkerEntity : ModifiableWorkspaceEntity<PackageMarkerEntity> {
  override var entitySource: EntitySource
  var root: VirtualFileUrl
  var packagePrefix: String
  var module: ModifiableModuleEntity
}

internal object PackageMarkerEntityType : EntityType<PackageMarkerEntity, ModifiablePackageMarkerEntity>() {
  override val entityClass: Class<PackageMarkerEntity> get() = PackageMarkerEntity::class.java
  operator fun invoke(
    root: VirtualFileUrl,
    packagePrefix: String,
    entitySource: EntitySource,
    init: (ModifiablePackageMarkerEntity.() -> Unit)? = null,
  ): ModifiablePackageMarkerEntity {
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
  modification: ModifiablePackageMarkerEntity.() -> Unit,
): PackageMarkerEntity = modifyEntity(ModifiablePackageMarkerEntity::class.java, entity, modification)

var ModifiableModuleEntity.packageMarkerEntities: List<ModifiablePackageMarkerEntity>
  by WorkspaceEntity.extensionBuilder(PackageMarkerEntity::class.java)


@JvmOverloads
@JvmName("createPackageMarkerEntity")
fun PackageMarkerEntity(
  root: VirtualFileUrl,
  packagePrefix: String,
  entitySource: EntitySource,
  init: (ModifiablePackageMarkerEntity.() -> Unit)? = null,
): ModifiablePackageMarkerEntity = PackageMarkerEntityType(root, packagePrefix, entitySource, init)
