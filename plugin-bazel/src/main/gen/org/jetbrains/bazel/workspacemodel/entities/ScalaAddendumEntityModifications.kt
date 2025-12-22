@file:JvmName("ScalaAddendumEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface ScalaAddendumEntityBuilder : WorkspaceEntityBuilder<ScalaAddendumEntity> {
  override var entitySource: EntitySource
  var compilerVersion: String
  var scalacOptions: MutableList<String>
  var sdkClasspaths: MutableList<VirtualFileUrl>
  var module: ModuleEntityBuilder
}

internal object ScalaAddendumEntityType : EntityType<ScalaAddendumEntity, ScalaAddendumEntityBuilder>() {
  override val entityClass: Class<ScalaAddendumEntity> get() = ScalaAddendumEntity::class.java
  operator fun invoke(
    compilerVersion: String,
    scalacOptions: List<String>,
    sdkClasspaths: List<VirtualFileUrl>,
    entitySource: EntitySource,
    init: (ScalaAddendumEntityBuilder.() -> Unit)? = null,
  ): ScalaAddendumEntityBuilder {
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
  modification: ScalaAddendumEntityBuilder.() -> Unit,
): ScalaAddendumEntity = modifyEntity(ScalaAddendumEntityBuilder::class.java, entity, modification)

var ModuleEntityBuilder.scalaAddendumEntity: ScalaAddendumEntityBuilder?
  by WorkspaceEntity.extensionBuilder(ScalaAddendumEntity::class.java)


@JvmOverloads
@JvmName("createScalaAddendumEntity")
fun ScalaAddendumEntity(
  compilerVersion: String,
  scalacOptions: List<String>,
  sdkClasspaths: List<VirtualFileUrl>,
  entitySource: EntitySource,
  init: (ScalaAddendumEntityBuilder.() -> Unit)? = null,
): ScalaAddendumEntityBuilder = ScalaAddendumEntityType(compilerVersion, scalacOptions, sdkClasspaths, entitySource, init)
