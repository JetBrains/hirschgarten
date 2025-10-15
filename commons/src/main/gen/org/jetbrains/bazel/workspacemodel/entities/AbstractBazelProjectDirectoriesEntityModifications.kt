@file:JvmName("AbstractBazelProjectDirectoriesEntityModifications")

package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.WorkspaceEntityBuilder
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

@GeneratedCodeApiVersion(3)
interface AbstractBazelProjectDirectoriesEntityBuilder<T : AbstractBazelProjectDirectoriesEntity> : WorkspaceEntityBuilder<T> {
  override var entitySource: EntitySource
  var projectRoot: VirtualFileUrl
}

internal object AbstractBazelProjectDirectoriesEntityType : EntityType<AbstractBazelProjectDirectoriesEntity, AbstractBazelProjectDirectoriesEntityBuilder<AbstractBazelProjectDirectoriesEntity>>() {
  override val entityClass: Class<AbstractBazelProjectDirectoriesEntity> get() = AbstractBazelProjectDirectoriesEntity::class.java
  operator fun invoke(
    projectRoot: VirtualFileUrl,
    entitySource: EntitySource,
    init: (AbstractBazelProjectDirectoriesEntityBuilder<AbstractBazelProjectDirectoriesEntity>.() -> Unit)? = null,
  ): AbstractBazelProjectDirectoriesEntityBuilder<AbstractBazelProjectDirectoriesEntity> {
    val builder = builder()
    builder.projectRoot = projectRoot
    builder.entitySource = entitySource
    init?.invoke(builder)
    return builder
  }
}
