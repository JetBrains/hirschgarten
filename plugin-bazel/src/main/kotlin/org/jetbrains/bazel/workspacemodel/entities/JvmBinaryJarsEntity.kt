package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Child
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.annotations.PublicApi

@PublicApi
interface JvmBinaryJarsEntity : WorkspaceEntity {
  val jars: List<VirtualFileUrl>
  val module: ModuleEntity

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<JvmBinaryJarsEntity> {
    override var entitySource: EntitySource
    var jars: MutableList<VirtualFileUrl>
    var module: ModuleEntity.Builder
  }

  companion object : EntityType<JvmBinaryJarsEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      jars: List<VirtualFileUrl>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.jars = jars.toMutableWorkspaceList()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyJvmBinaryJarsEntity(
  entity: JvmBinaryJarsEntity,
  modification: JvmBinaryJarsEntity.Builder.() -> Unit,
): JvmBinaryJarsEntity = modifyEntity(JvmBinaryJarsEntity.Builder::class.java, entity, modification)

var ModuleEntity.Builder.jvmBinaryJarsEntity: @Child JvmBinaryJarsEntity.Builder?
  by WorkspaceEntity.extensionBuilder(JvmBinaryJarsEntity::class.java)
//endregion

@PublicApi
val ModuleEntity.jvmBinaryJarsEntity: @Child JvmBinaryJarsEntity? by WorkspaceEntity.extension()
