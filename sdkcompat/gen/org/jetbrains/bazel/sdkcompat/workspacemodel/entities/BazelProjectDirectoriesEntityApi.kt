package org.jetbrains.bazel.sdkcompat.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.ModifiableWorkspaceEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bazel.workspacemodel.entities.AbstractBazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.ModifiableAbstractBazelProjectDirectoriesEntity

@GeneratedCodeApiVersion(3)
interface ModifiableBazelProjectDirectoriesEntity : ModifiableWorkspaceEntity<BazelProjectDirectoriesEntity>,
                                                    ModifiableAbstractBazelProjectDirectoriesEntity<BazelProjectDirectoriesEntity> {
  override var entitySource: EntitySource
  override var projectRoot: VirtualFileUrl
  var includedRoots: MutableList<VirtualFileUrl>
  var excludedRoots: MutableList<VirtualFileUrl>
  var indexAllFilesInIncludedRoots: Boolean
  var indexAdditionalFiles: MutableList<VirtualFileUrl>
}

internal object BazelProjectDirectoriesEntityType : EntityType<BazelProjectDirectoriesEntity, ModifiableBazelProjectDirectoriesEntity>() {
  override val entityClass: Class<BazelProjectDirectoriesEntity> get() = BazelProjectDirectoriesEntity::class.java
  operator fun invoke(
    projectRoot: VirtualFileUrl,
    includedRoots: List<VirtualFileUrl>,
    excludedRoots: List<VirtualFileUrl>,
    indexAllFilesInIncludedRoots: Boolean,
    indexAdditionalFiles: List<VirtualFileUrl>,
    entitySource: EntitySource,
    init: (ModifiableBazelProjectDirectoriesEntity.() -> Unit)? = null,
  ): ModifiableBazelProjectDirectoriesEntity {
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
  modification: ModifiableBazelProjectDirectoriesEntity.() -> Unit,
): BazelProjectDirectoriesEntity = modifyEntity(ModifiableBazelProjectDirectoriesEntity::class.java, entity, modification)

@JvmOverloads
@JvmName("createBazelProjectDirectoriesEntity")
fun BazelProjectDirectoriesEntity(
  projectRoot: VirtualFileUrl,
  includedRoots: List<VirtualFileUrl>,
  excludedRoots: List<VirtualFileUrl>,
  indexAllFilesInIncludedRoots: Boolean,
  indexAdditionalFiles: List<VirtualFileUrl>,
  entitySource: EntitySource,
  init: (ModifiableBazelProjectDirectoriesEntity.() -> Unit)? = null,
): ModifiableBazelProjectDirectoriesEntity =
  BazelProjectDirectoriesEntityType(projectRoot, includedRoots, excludedRoots, indexAllFilesInIncludedRoots, indexAdditionalFiles,
                                    entitySource, init)
