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
  @GeneratedCodeApiVersion(2)
  public interface Builder : BspProjectDirectoriesEntity, WorkspaceEntity.Builder<BspProjectDirectoriesEntity> {
    override var entitySource: EntitySource
    override var projectRoot: VirtualFileUrl
    override var includedRoots: MutableList<VirtualFileUrl>
    override var excludedRoots: MutableList<VirtualFileUrl>
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
    ): BspProjectDirectoriesEntity {
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
public fun MutableEntityStorage.modifyEntity(
  entity: BspProjectDirectoriesEntity,
  modification: BspProjectDirectoriesEntity.Builder.() -> Unit,
): BspProjectDirectoriesEntity = modifyEntity(BspProjectDirectoriesEntity.Builder::class.java, entity, modification)
//endregion
