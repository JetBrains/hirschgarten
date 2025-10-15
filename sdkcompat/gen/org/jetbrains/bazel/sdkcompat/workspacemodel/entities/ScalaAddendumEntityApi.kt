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
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.annotations.PublicApi

@GeneratedCodeApiVersion(3)
interface ModifiableScalaAddendumEntity : ModifiableWorkspaceEntity<ScalaAddendumEntity> {
  override var entitySource: EntitySource
  var compilerVersion: String
  var scalacOptions: MutableList<String>
  var sdkClasspaths: MutableList<VirtualFileUrl>
  var module: ModifiableModuleEntity
}

internal object ScalaAddendumEntityType : EntityType<ScalaAddendumEntity, ModifiableScalaAddendumEntity>() {
  override val entityClass: Class<ScalaAddendumEntity> get() = ScalaAddendumEntity::class.java
  operator fun invoke(
    compilerVersion: String,
    scalacOptions: List<String>,
    sdkClasspaths: List<VirtualFileUrl>,
    entitySource: EntitySource,
    init: (ModifiableScalaAddendumEntity.() -> Unit)? = null,
  ): ModifiableScalaAddendumEntity {
    val builder = builder()
    builder.compilerVersion = compilerVersion
    builder.scalacOptions = scalacOptions.toMutableWorkspaceList()
    builder.sdkClasspaths = sdkClasspaths.toMutableWorkspaceList()
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

fun MutableEntityStorage.modifyScalaAddendumEntity(
  entity: ScalaAddendumEntity,
  modification: ModifiableScalaAddendumEntity.() -> Unit,
): ScalaAddendumEntity = modifyEntity(ModifiableScalaAddendumEntity::class.java, entity, modification)

var ModifiableModuleEntity.scalaAddendumEntity: ModifiableScalaAddendumEntity?
  by WorkspaceEntity.extensionBuilder(ScalaAddendumEntity::class.java)


@JvmOverloads
@JvmName("createScalaAddendumEntity")
fun ScalaAddendumEntity(
  compilerVersion: String,
  scalacOptions: List<String>,
  sdkClasspaths: List<VirtualFileUrl>,
  entitySource: EntitySource,
  init: (ModifiableScalaAddendumEntity.() -> Unit)? = null,
): ModifiableScalaAddendumEntity = ScalaAddendumEntityType(compilerVersion, scalacOptions, sdkClasspaths, entitySource, init)
