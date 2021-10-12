package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.WorkspaceEntityStorageBuilder
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRoot
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRootTypeId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import com.intellij.workspaceModel.storage.bridgeEntities.addLibraryEntity

internal data class Library(
  val displayName: String,
  val jar: String,
) : WorkspaceModelEntity()

internal class LibraryEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<Library, LibraryEntity> {

  override fun addEntity(entityToAdd: Library, parentModuleEntity: ModuleEntity): LibraryEntity {
    return workspaceModelEntityUpdaterConfig.workspaceModel.updateProjectModel {
      addLibraryEntity(it, parentModuleEntity, entityToAdd)
    }
  }

  private fun addLibraryEntity(
    builder: WorkspaceEntityStorageBuilder,
    parentModuleEntity: ModuleEntity,
    entityToAdd: Library,
  ): LibraryEntity =
    builder.addLibraryEntity(
      name = entityToAdd.displayName,
      tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
      listOf(toLibraryRoot(entityToAdd)),
      emptyList(),
      workspaceModelEntityUpdaterConfig.projectConfigSource
    )

  private fun toLibraryRoot(entityToAdd: Library): LibraryRoot =
    LibraryRoot(
      url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl(entityToAdd.jar),
      type = LibraryRootTypeId.SOURCES,
    )
}
