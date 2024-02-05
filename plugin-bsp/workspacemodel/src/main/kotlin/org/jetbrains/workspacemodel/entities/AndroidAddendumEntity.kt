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

public enum class AndroidTargetType {
  APP,
  LIBRARY,
  TEST,
}

public interface AndroidAddendumEntity : WorkspaceEntity {
  public val androidSdkName: String
  public val androidTargetType: AndroidTargetType
  public val manifest: VirtualFileUrl?
  public val resourceFolders: List<VirtualFileUrl>
  public val module: ModuleEntity

  //region generated code
  @GeneratedCodeApiVersion(2)
  public interface Builder : AndroidAddendumEntity, WorkspaceEntity.Builder<AndroidAddendumEntity> {
    override var entitySource: EntitySource
    override var androidSdkName: String
    override var androidTargetType: AndroidTargetType
    override var manifest: VirtualFileUrl?
    override var resourceFolders: MutableList<VirtualFileUrl>
    override var module: ModuleEntity
  }

  public companion object : EntityType<AndroidAddendumEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    public operator fun invoke(
      androidSdkName: String,
      androidTargetType: AndroidTargetType,
      resourceFolders: List<VirtualFileUrl>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): AndroidAddendumEntity {
      val builder = builder()
      builder.androidSdkName = androidSdkName
      builder.androidTargetType = androidTargetType
      builder.resourceFolders = resourceFolders.toMutableWorkspaceList()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
//endregion
}

//region generated code
public fun MutableEntityStorage.modifyEntity(
  entity: AndroidAddendumEntity,
  modification: AndroidAddendumEntity.Builder.() -> Unit,
): AndroidAddendumEntity = modifyEntity(AndroidAddendumEntity.Builder::class.java, entity, modification)

public var ModuleEntity.Builder.androidAddendumEntity: @Child AndroidAddendumEntity?
  by WorkspaceEntity.extension()
//endregion

public val ModuleEntity.androidAddendumEntity: @Child AndroidAddendumEntity? by WorkspaceEntity.extension()
