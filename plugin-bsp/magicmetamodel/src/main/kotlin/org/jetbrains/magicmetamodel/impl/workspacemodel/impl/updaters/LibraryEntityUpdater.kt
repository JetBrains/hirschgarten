package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryEntity
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRoot
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryRootTypeId
import com.intellij.workspaceModel.storage.bridgeEntities.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleId
import com.intellij.workspaceModel.storage.bridgeEntities.addLibraryEntity

internal data class Library(
  val displayName: String,
  val sourcesJar: String?,
  val classesJar: String?,
) : WorkspaceModelEntity()

internal class LibraryEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithParentModuleUpdater<Library, LibraryEntity> {

  override fun addEntity(entityToAdd: Library, parentModuleEntity: ModuleEntity): LibraryEntity =
    addLibraryEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, parentModuleEntity, entityToAdd)

  private fun addLibraryEntity(
    builder: MutableEntityStorage,
    parentModuleEntity: ModuleEntity,
    entityToAdd: Library,
  ): LibraryEntity =
    builder.addLibraryEntity(
      name = entityToAdd.displayName,
      tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
      roots = listOfNotNull(toLibrarySourcesRoot(entityToAdd), toLibraryClassesRoot(entityToAdd)),
      excludedRoots = ArrayList(),
      source = DoNotSaveInDotIdeaDirEntitySource
    )

  private fun toLibrarySourcesRoot(entityToAdd: Library): LibraryRoot? =
    entityToAdd.sourcesJar?.let {
      LibraryRoot(
        url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl(it),
        type = LibraryRootTypeId.SOURCES,
      )
    }

  private fun toLibraryClassesRoot(entityToAdd: Library): LibraryRoot? =
    entityToAdd.classesJar?.let {
      LibraryRoot(
        url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl(it),
        type = LibraryRootTypeId.COMPILED,
      )
    }
}
