package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.EntityType
import com.intellij.platform.workspace.storage.GeneratedCodeApiVersion
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.WorkspaceEntityWithSymbolicId
import com.intellij.platform.workspace.storage.impl.containers.toMutableWorkspaceList
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

public data class PackageNameId(public val packageName: String) : SymbolicEntityId<WorkspaceEntityWithSymbolicId> {
  override val presentableName: String
    get() = packageName
}

public interface BazelJavaSourceRootEntity : WorkspaceEntity {
  public val packageNameId: PackageNameId
  public val sourceRoots: List<VirtualFileUrl>

  //region generated code
  @GeneratedCodeApiVersion(3)
  interface Builder : WorkspaceEntity.Builder<BazelJavaSourceRootEntity> {
    override var entitySource: EntitySource
    var packageNameId: PackageNameId
    var sourceRoots: MutableList<VirtualFileUrl>
  }

  companion object : EntityType<BazelJavaSourceRootEntity, Builder>() {
    @JvmOverloads
    @JvmStatic
    @JvmName("create")
    operator fun invoke(
      packageNameId: PackageNameId,
      sourceRoots: List<VirtualFileUrl>,
      entitySource: EntitySource,
      init: (Builder.() -> Unit)? = null,
    ): Builder {
      val builder = builder()
      builder.packageNameId = packageNameId
      builder.sourceRoots = sourceRoots.toMutableWorkspaceList()
      builder.entitySource = entitySource
      init?.invoke(builder)
      return builder
    }
  }
  //endregion
}

//region generated code
fun MutableEntityStorage.modifyBazelJavaSourceRootEntity(
  entity: BazelJavaSourceRootEntity,
  modification: BazelJavaSourceRootEntity.Builder.() -> Unit,
): BazelJavaSourceRootEntity = modifyEntity(BazelJavaSourceRootEntity.Builder::class.java, entity, modification)
//endregion
