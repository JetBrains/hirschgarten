package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Child
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

interface PackageMarkerEntity : WorkspaceEntity {
  val root: VirtualFileUrl
  val packagePrefix: String
  val module: ModuleEntity

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<PackageMarkerEntity> {
    override var entitySource: EntitySource
    var root: VirtualFileUrl
    var packagePrefix: String
    var module: ModuleEntity.Builder
  }

  companion object : EntityType<PackageMarkerEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      root: VirtualFileUrl,
      packagePrefix: String,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.root = root
      builder.packagePrefix = packagePrefix
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyPackageMarkerEntity(
  entity: PackageMarkerEntity,
  modification: PackageMarkerEntity.Builder.() -> Unit,
): PackageMarkerEntity = modifyEntity(PackageMarkerEntity.Builder::class.java, entity, modification)

var ModuleEntity.Builder.packageMarkerEntities: @Child List<PackageMarkerEntity.Builder>
  by WorkspaceEntity.extensionBuilder(PackageMarkerEntity::class.java)
//endregion

val ModuleEntity.packageMarkerEntities: List<@Child PackageMarkerEntity>
  by WorkspaceEntity.extension()
