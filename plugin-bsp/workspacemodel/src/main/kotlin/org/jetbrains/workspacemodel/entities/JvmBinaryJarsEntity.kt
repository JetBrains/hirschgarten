package org.jetbrains.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Child
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

public interface JvmBinaryJarsEntity : WorkspaceEntity {
  public val jars: List<VirtualFileUrl>
  public val module: ModuleEntity

  //region generated code
  @GeneratedCodeApiVersion(2)
  public interface Builder : JvmBinaryJarsEntity, WorkspaceEntity.Builder<JvmBinaryJarsEntity> {
    override var entitySource: EntitySource
    override var jars: MutableList<VirtualFileUrl>
    override var module: ModuleEntity
  }

  public companion object : EntityType<JvmBinaryJarsEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    public operator fun invoke(
      jars: List<VirtualFileUrl>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): JvmBinaryJarsEntity {
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
public fun MutableEntityStorage.modifyEntity(
  entity: JvmBinaryJarsEntity,
  modification: JvmBinaryJarsEntity.Builder.() -> Unit,
): JvmBinaryJarsEntity = modifyEntity(JvmBinaryJarsEntity.Builder::class.java, entity, modification)

public var ModuleEntity.Builder.jvmBinaryJarsEntity: @Child JvmBinaryJarsEntity?
  by WorkspaceEntity.extension()
//endregion

public val ModuleEntity.jvmBinaryJarsEntity: @Child JvmBinaryJarsEntity? by WorkspaceEntity.extension()
