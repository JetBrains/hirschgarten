package org.jetbrains.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

public interface BspProjectDirectoriesEntity : WorkspaceEntity {
  public val projectRoot: VirtualFileUrl
  public val includedRoots: List<VirtualFileUrl>
  public val excludedRoots: List<VirtualFileUrl>

  //region generated code
  @GeneratedCodeApiVersion(3)
  public interface Builder : WorkspaceEntity.Builder<BspProjectDirectoriesEntity> {
    override var entitySource: EntitySource
    var projectRoot: VirtualFileUrl
    var includedRoots: MutableList<VirtualFileUrl>
    var excludedRoots: MutableList<VirtualFileUrl>
  }

  public companion object : EntityType<BspProjectDirectoriesEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    public operator fun invoke(
      projectRoot: VirtualFileUrl,
      includedRoots: List<VirtualFileUrl>,
      excludedRoots: List<VirtualFileUrl>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.projectRoot = projectRoot
      builder.includedRoots = includedRoots.toMutableWorkspaceList()
      builder.excludedRoots = excludedRoots.toMutableWorkspaceList()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
//endregion
}

//region generated code
public fun MutableEntityStorage.modifyBspProjectDirectoriesEntity(
  entity: BspProjectDirectoriesEntity,
  modification: BspProjectDirectoriesEntity.Builder.() -> Unit,
): BspProjectDirectoriesEntity {
  return modifyEntity(BspProjectDirectoriesEntity.Builder::class.java, entity, modification)
}
//endregion
