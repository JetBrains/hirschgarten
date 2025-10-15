package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.ModifiableWorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Abstract
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface ModifiableAbstractBazelProjectDirectoriesEntity<T : AbstractBazelProjectDirectoriesEntity> : ModifiableWorkspaceEntity<T> {
  override var entitySource: EntitySource
  var projectRoot: VirtualFileUrl
}

internal object AbstractBazelProjectDirectoriesEntityType : EntityType<AbstractBazelProjectDirectoriesEntity, ModifiableAbstractBazelProjectDirectoriesEntity<AbstractBazelProjectDirectoriesEntity>>() {
  override val entityClass: Class<AbstractBazelProjectDirectoriesEntity> get() = AbstractBazelProjectDirectoriesEntity::class.java
  operator fun invoke(
    projectRoot: VirtualFileUrl,
    entitySource: EntitySource,
    init: (ModifiableAbstractBazelProjectDirectoriesEntity<AbstractBazelProjectDirectoriesEntity>.() -> Unit)? = null,
  ): ModifiableAbstractBazelProjectDirectoriesEntity<AbstractBazelProjectDirectoriesEntity> {
    val builder = builder()
    builder.projectRoot = projectRoot
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}
