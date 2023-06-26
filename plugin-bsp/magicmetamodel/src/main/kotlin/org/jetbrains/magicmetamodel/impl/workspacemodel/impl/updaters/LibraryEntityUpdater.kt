package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId

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
    builder.addEntity(
      LibraryEntity(
        name = entityToAdd.displayName,
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name)),
        roots = listOfNotNull(toLibrarySourcesRoot(entityToAdd), toLibraryClassesRoot(entityToAdd)),
        entitySource = BspEntitySource
      ) {
        this.excludedRoots = arrayListOf()
      }
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
