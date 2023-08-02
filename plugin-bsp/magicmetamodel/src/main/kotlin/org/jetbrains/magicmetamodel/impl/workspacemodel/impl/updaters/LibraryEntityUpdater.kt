package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library

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
        roots = toLibrarySourcesRoots(entityToAdd) + toLibraryClassesRoots(entityToAdd),
        entitySource = BspEntitySource
      ) {
        this.excludedRoots = arrayListOf()
      }
    )

  private fun toLibrarySourcesRoots(entityToAdd: Library): List<LibraryRoot> =
    entityToAdd.sourceJars.map {
      LibraryRoot(
        url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl(it),
        type = LibraryRootTypeId.SOURCES
      )
    }

  private fun toLibraryClassesRoots(entityToAdd: Library): List<LibraryRoot> =
    entityToAdd.classJars.map {
      LibraryRoot(
        url = workspaceModelEntityUpdaterConfig.virtualFileUrlManager.fromUrl(it),
        type = LibraryRootTypeId.COMPILED
      )
    }
}
