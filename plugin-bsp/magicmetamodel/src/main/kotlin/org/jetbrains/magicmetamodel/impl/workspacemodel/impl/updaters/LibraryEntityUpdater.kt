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
  val sourcesJar: String,
  val classesJar: String,
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
      listOf(toLibrarySourcesRoot(entityToAdd), toLibraryClassesRoot(entityToAdd)),
      emptyList(),
      workspaceModelEntityUpdaterConfig.projectConfigSource
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
