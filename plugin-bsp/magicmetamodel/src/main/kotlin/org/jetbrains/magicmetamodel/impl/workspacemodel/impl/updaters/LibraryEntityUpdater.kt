package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryRoot
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryRootTypeId
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleId
import com.intellij.workspaceModel.storage.bridgeEntities.addLibraryEntity

internal data class Library(
  val displayName: String,
  val sourcesJar: String,
  val classesJar: String,
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
      roots = listOf(toLibrarySourcesRoot(entityToAdd), toLibraryClassesRoot(entityToAdd)),
      excludedRoots = ArrayList(),
      source = DoNotSaveInDotIdeaDirEntitySource
    )

  private fun toLibrarySourcesRoot(entityToAdd: Library): LibraryRoot =
    LibraryRoot(
      url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl(entityToAdd.sourcesJar),
      type = LibraryRootTypeId.SOURCES,
    )

  private fun toLibraryClassesRoot(entityToAdd: Library): LibraryRoot =
    LibraryRoot(
      url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl(entityToAdd.classesJar),
      type = LibraryRootTypeId.COMPILED,
    )
}
