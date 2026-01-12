@file:JvmName("BazelProjectDirectoriesEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface BazelProjectDirectoriesEntityBuilder: WorkspaceEntityBuilder<BazelProjectDirectoriesEntity>{
override var entitySource: EntitySource
var projectRoot: VirtualFileUrl
var includedRoots: MutableList<VirtualFileUrl>
var excludedRoots: MutableList<VirtualFileUrl>
var indexAllFilesInIncludedRoots: Boolean
var indexAdditionalFiles: MutableList<VirtualFileUrl>
}

internal object BazelProjectDirectoriesEntityType : EntityType<BazelProjectDirectoriesEntity, BazelProjectDirectoriesEntityBuilder>(){
override val entityClass: Class<BazelProjectDirectoriesEntity> get() = BazelProjectDirectoriesEntity::class.java
operator fun invoke(
projectRoot: VirtualFileUrl,
includedRoots: List<VirtualFileUrl>,
excludedRoots: List<VirtualFileUrl>,
indexAllFilesInIncludedRoots: Boolean,
indexAdditionalFiles: List<VirtualFileUrl>,
entitySource: EntitySource,
init: (BazelProjectDirectoriesEntityBuilder.() -> Unit)? = null,
): BazelProjectDirectoriesEntityBuilder{
val builder = builder()
builder.projectRoot = projectRoot
builder.includedRoots = includedRoots.toMutableWorkspaceList()
builder.excludedRoots = excludedRoots.toMutableWorkspaceList()
builder.indexAllFilesInIncludedRoots = indexAllFilesInIncludedRoots
builder.indexAdditionalFiles = indexAdditionalFiles.toMutableWorkspaceList()
builder.entitySource = entitySource
init?.invoke(builder)
return builder
}
}

fun MutableEntityStorage.modifyBazelProjectDirectoriesEntity(
entity: BazelProjectDirectoriesEntity,
modification: BazelProjectDirectoriesEntityBuilder.() -> Unit,
): BazelProjectDirectoriesEntity = modifyEntity(BazelProjectDirectoriesEntityBuilder::class.java, entity, modification)

@JvmOverloads
@JvmName("createBazelProjectDirectoriesEntity")
fun BazelProjectDirectoriesEntity(
projectRoot: VirtualFileUrl,
includedRoots: List<VirtualFileUrl>,
excludedRoots: List<VirtualFileUrl>,
indexAllFilesInIncludedRoots: Boolean,
indexAdditionalFiles: List<VirtualFileUrl>,
entitySource: EntitySource,
init: (BazelProjectDirectoriesEntityBuilder.() -> Unit)? = null,
): BazelProjectDirectoriesEntityBuilder = BazelProjectDirectoriesEntityType(projectRoot, includedRoots, excludedRoots, indexAllFilesInIncludedRoots, indexAdditionalFiles, entitySource, init)
