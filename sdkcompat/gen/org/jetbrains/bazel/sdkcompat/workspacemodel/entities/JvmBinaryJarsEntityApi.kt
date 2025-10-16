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
interface ModifiableJvmBinaryJarsEntity : ModifiableWorkspaceEntity<JvmBinaryJarsEntity> {
  override var entitySource: EntitySource
  var jars: MutableList<VirtualFileUrl>
  var module: ModifiableModuleEntity
}

internal object JvmBinaryJarsEntityType : EntityType<JvmBinaryJarsEntity, ModifiableJvmBinaryJarsEntity>() {
  override val entityClass: Class<JvmBinaryJarsEntity> get() = JvmBinaryJarsEntity::class.java
  operator fun invoke(
    jars: List<VirtualFileUrl>,
    entitySource: EntitySource,
    init: (ModifiableJvmBinaryJarsEntity.() -> Unit)? = null,
  ): ModifiableJvmBinaryJarsEntity {
    val builder = builder()
    builder.jars = jars.toMutableWorkspaceList()
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}

fun MutableEntityStorage.modifyJvmBinaryJarsEntity(
  entity: JvmBinaryJarsEntity,
  modification: ModifiableJvmBinaryJarsEntity.() -> Unit,
): JvmBinaryJarsEntity = modifyEntity(ModifiableJvmBinaryJarsEntity::class.java, entity, modification)

var ModifiableModuleEntity.jvmBinaryJarsEntity: ModifiableJvmBinaryJarsEntity?
  by WorkspaceEntity.extensionBuilder(JvmBinaryJarsEntity::class.java)


@JvmOverloads
@JvmName("createJvmBinaryJarsEntity")
fun JvmBinaryJarsEntity(
  jars: List<VirtualFileUrl>,
  entitySource: EntitySource,
  init: (ModifiableJvmBinaryJarsEntity.() -> Unit)? = null,
): ModifiableJvmBinaryJarsEntity = JvmBinaryJarsEntityType(jars, entitySource, init)
