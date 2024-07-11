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
  public val resourceDirectories: List<VirtualFileUrl>
  public val resourceJavaPackage: String?
  public val assetsDirectories: List<VirtualFileUrl>
  public val module: ModuleEntity

  //region generated code
  @GeneratedCodeApiVersion(3)
  public interface Builder : WorkspaceEntity.Builder<AndroidAddendumEntity> {
    override var entitySource: EntitySource
    var androidSdkName: String
    var androidTargetType: AndroidTargetType
    var manifest: VirtualFileUrl?
    var resourceDirectories: MutableList<VirtualFileUrl>
    var resourceJavaPackage: String?
    var assetsDirectories: MutableList<VirtualFileUrl>
    var module: ModuleEntity.Builder
  }

  public companion object : EntityType<AndroidAddendumEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    public operator fun invoke(
      androidSdkName: String,
      androidTargetType: AndroidTargetType,
      resourceDirectories: List<VirtualFileUrl>,
      assetsDirectories: List<VirtualFileUrl>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.androidSdkName = androidSdkName
      builder.androidTargetType = androidTargetType
      builder.resourceDirectories = resourceDirectories.toMutableWorkspaceList()
      builder.assetsDirectories = assetsDirectories.toMutableWorkspaceList()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
//endregion

}

//region generated code
public fun MutableEntityStorage.modifyAndroidAddendumEntity(
  entity: AndroidAddendumEntity,
  modification: AndroidAddendumEntity.Builder.() -> Unit,
): AndroidAddendumEntity {
  return modifyEntity(AndroidAddendumEntity.Builder::class.java, entity, modification)
}

public var ModuleEntity.Builder.androidAddendumEntity: @Child AndroidAddendumEntity.Builder?
    by WorkspaceEntity.extensionBuilder(AndroidAddendumEntity::class.java)
//endregion

public val ModuleEntity.androidAddendumEntity: @Child AndroidAddendumEntity? by WorkspaceEntity.extension()