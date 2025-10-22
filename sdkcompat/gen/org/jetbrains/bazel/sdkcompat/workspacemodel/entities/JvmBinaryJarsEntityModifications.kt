@file:JvmName("JvmBinaryJarsEntityModifications")

package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface JvmBinaryJarsEntityBuilder : WorkspaceEntityBuilder<JvmBinaryJarsEntity> {
  override var entitySource: EntitySource
  var jars: MutableList<VirtualFileUrl>
  var module: ModuleEntityBuilder
}

internal object JvmBinaryJarsEntityType : EntityType<JvmBinaryJarsEntity, JvmBinaryJarsEntityBuilder>() {
  override val entityClass: Class<JvmBinaryJarsEntity> get() = JvmBinaryJarsEntity::class.java
  operator fun invoke(
    jars: List<VirtualFileUrl>,
    entitySource: EntitySource,
    init: (JvmBinaryJarsEntityBuilder.() -> Unit)? = null,
  ): JvmBinaryJarsEntityBuilder {
    val builder = builder()
    builder.jars = jars.toMutableWorkspaceList()
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

fun MutableEntityStorage.modifyJvmBinaryJarsEntity(
  entity: JvmBinaryJarsEntity,
  modification: JvmBinaryJarsEntityBuilder.() -> Unit,
): JvmBinaryJarsEntity = modifyEntity(JvmBinaryJarsEntityBuilder::class.java, entity, modification)

var ModuleEntityBuilder.jvmBinaryJarsEntity: JvmBinaryJarsEntityBuilder?
  by WorkspaceEntity.extensionBuilder(JvmBinaryJarsEntity::class.java)


@JvmOverloads
@JvmName("createJvmBinaryJarsEntity")
fun JvmBinaryJarsEntity(
  jars: List<VirtualFileUrl>,
  entitySource: EntitySource,
  init: (JvmBinaryJarsEntityBuilder.() -> Unit)? = null,
): JvmBinaryJarsEntityBuilder = JvmBinaryJarsEntityType(jars, entitySource, init)
