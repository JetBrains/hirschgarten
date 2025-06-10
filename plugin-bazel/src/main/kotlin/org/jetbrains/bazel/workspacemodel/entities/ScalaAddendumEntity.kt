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

interface ScalaAddendumEntity : WorkspaceEntity {
  val compilerVersion: String
  val scalacOptions: List<String>
  val sdkClasspaths: List<VirtualFileUrl>
  val module: ModuleEntity

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<ScalaAddendumEntity> {
    override var entitySource: EntitySource
    var compilerVersion: String
    var scalacOptions: MutableList<String>
    var sdkClasspaths: MutableList<VirtualFileUrl>
    var module: ModuleEntity.Builder
  }

  companion object : EntityType<ScalaAddendumEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      compilerVersion: String,
      scalacOptions: List<String>,
      sdkClasspaths: List<VirtualFileUrl>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.compilerVersion = compilerVersion
      builder.scalacOptions = scalacOptions.toMutableWorkspaceList()
      builder.sdkClasspaths = sdkClasspaths.toMutableWorkspaceList()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyScalaAddendumEntity(
  entity: ScalaAddendumEntity,
  modification: ScalaAddendumEntity.Builder.() -> Unit,
): ScalaAddendumEntity = modifyEntity(ScalaAddendumEntity.Builder::class.java, entity, modification)

var ModuleEntity.Builder.scalaAddendumEntity: @Child ScalaAddendumEntity.Builder?
  by WorkspaceEntity.extensionBuilder(ScalaAddendumEntity::class.java)
//endregion

val ModuleEntity.scalaAddendumEntity: @Child ScalaAddendumEntity? by WorkspaceEntity.extension()
