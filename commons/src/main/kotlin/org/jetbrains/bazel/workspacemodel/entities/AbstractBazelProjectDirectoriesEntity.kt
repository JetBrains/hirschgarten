package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.annotations.Abstract
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

// todo to be removed
//   workaround for sdkcompat
@Abstract
interface AbstractBazelProjectDirectoriesEntity : WorkspaceEntity {
  val projectRoot: VirtualFileUrl

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder<T : AbstractBazelProjectDirectoriesEntity> : WorkspaceEntity.Builder<T> {
    override var entitySource: EntitySource
    var projectRoot: VirtualFileUrl
  }

  companion object : EntityType<AbstractBazelProjectDirectoriesEntity, Builder<AbstractBazelProjectDirectoriesEntity>>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      projectRoot: VirtualFileUrl,
      entitySource: EntitySource,
      init: (Builder<AbstractBazelProjectDirectoriesEntity>.() -> Unit)? = null,
    ): Builder<AbstractBazelProjectDirectoriesEntity> {
      val builder = builder()
      builder.projectRoot = projectRoot
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}
