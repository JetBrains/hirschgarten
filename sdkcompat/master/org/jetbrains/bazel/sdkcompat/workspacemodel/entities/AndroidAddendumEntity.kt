1package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Parent
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
  public val manifestOverrides: Map<String, String>
  public val resourceDirectories: List<VirtualFileUrl>
  public val resourceJavaPackage: String?
  public val assetsDirectories: List<VirtualFileUrl>
  public val apk: VirtualFileUrl?

  @Parent
  public val module: ModuleEntity

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<AndroidAddendumEntity> {
    override var entitySource: EntitySource
    var androidSdkName: String
    var androidTargetType: AndroidTargetType
    var manifest: VirtualFileUrl?
    var manifestOverrides: Map<String, String>
    var resourceDirectories: MutableList<VirtualFileUrl>
    var resourceJavaPackage: String?
    var assetsDirectories: MutableList<VirtualFileUrl>
    var apk: VirtualFileUrl?
    var module: ModuleEntity.Builder
  }

  companion object : EntityType<AndroidAddendumEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      androidSdkName: String,
      androidTargetType: AndroidTargetType,
      manifestOverrides: Map<String, String>,
      resourceDirectories: List<VirtualFileUrl>,
      assetsDirectories: List<VirtualFileUrl>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.androidSdkName = androidSdkName
      builder.androidTargetType = androidTargetType
      builder.manifestOverrides = manifestOverrides
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
fun MutableEntityStorage.modifyAndroidAddendumEntity(
  entity: AndroidAddendumEntity,
  modification: AndroidAddendumEntity.Builder.() -> Unit,
): AndroidAddendumEntity = modifyEntity(AndroidAddendumEntity.Builder::class.java, entity, modification)

var ModuleEntity.Builder.androidAddendumEntity: AndroidAddendumEntity.Builder?
  by WorkspaceEntity.extensionBuilder(AndroidAddendumEntity::class.java)
//endregion

public val ModuleEntity.androidAddendumEntity: AndroidAddendumEntity? by WorkspaceEntity.extension()
