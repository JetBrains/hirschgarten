@file:JvmName("BazelModuleExtensionEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntityBuilder
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.annotations.Parent
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.bazel.label.Label

@Internal
@GeneratedCodeApiVersion(3)
interface BazelModuleExtensionEntityBuilder: WorkspaceEntityBuilder<BazelModuleExtensionEntity>{
override var entitySource: EntitySource
var module: ModuleEntityBuilder
var label: WorkspaceModelTargetLabel
}

internal object BazelModuleExtensionEntityType : EntityType<BazelModuleExtensionEntity, BazelModuleExtensionEntityBuilder>(){
override val entityClass: Class<BazelModuleExtensionEntity> get() = BazelModuleExtensionEntity::class.java
operator fun invoke(
label: WorkspaceModelTargetLabel,
entitySource: EntitySource,
init: (BazelModuleExtensionEntityBuilder.() -> Unit)? = null,
): BazelModuleExtensionEntityBuilder{
val builder = builder()
builder.label = label
builder.entitySource = entitySource
init?.invoke(builder)
return builder
}
}

@Internal
fun MutableEntityStorage.modifyBazelModuleExtensionEntity(
entity: BazelModuleExtensionEntity,
modification: BazelModuleExtensionEntityBuilder.() -> Unit,
): BazelModuleExtensionEntity = modifyEntity(BazelModuleExtensionEntityBuilder::class.java, entity, modification)
@get:Internal
@set:Internal
var ModuleEntityBuilder.bazelModuleExtension: BazelModuleExtensionEntityBuilder?
by WorkspaceEntity.extensionBuilder(BazelModuleExtensionEntity::class.java)


@Internal
@JvmOverloads
@JvmName("createBazelModuleExtensionEntity")
fun BazelModuleExtensionEntity(
label: WorkspaceModelTargetLabel,
entitySource: EntitySource,
init: (BazelModuleExtensionEntityBuilder.() -> Unit)? = null,
): BazelModuleExtensionEntityBuilder = BazelModuleExtensionEntityType(label, entitySource, init)
