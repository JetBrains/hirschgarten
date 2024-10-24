package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.bsp.sdkcompat.workspacemodel.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.plugins.bsp.extensionPoints.bspProjectModelExternalSource
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspProjectEntitySource
import org.jetbrains.plugins.bsp.workspacemodel.entities.Library

internal class LibraryEntityUpdater(private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig) :
  WorkspaceModelEntityWithoutParentModuleUpdater<Library, LibraryEntity> {
  //  a snippet of adding module library entity in case we want it back
  //  private fun addModuleLibraryEntity(
  //    builder: MutableEntityStorage,
  //    parentModuleEntity: ModuleEntity,
  //    entityToAdd: Library,
  //  ): LibraryEntity {
  //    val tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(parentModuleEntity.name))
  //    val entitySource = parentModuleEntity.entitySource
  //    return addLibraryEntity(builder, entityToAdd, tableId, entitySource)
  //  }
  override fun addEntity(entityToAdd: Library): LibraryEntity =
    addProjectLibraryEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd)

  private fun addProjectLibraryEntity(builder: MutableEntityStorage, entityToAdd: Library): LibraryEntity {
    val tableId = LibraryTableId.ProjectLibraryTableId
    val entitySource = calculateLibraryEntitySource(workspaceModelEntityUpdaterConfig)
    return addLibraryEntity(builder, entityToAdd, tableId, entitySource)
  }

  private fun addLibraryEntity(
    builder: MutableEntityStorage,
    entityToAdd: Library,
    tableId: LibraryTableId,
    entitySource: EntitySource,
  ): LibraryEntity {
    val libraryEntity =
      LibraryEntity(
        name = entityToAdd.displayName,
        tableId = tableId,
        roots = toLibrarySourcesRoots(entityToAdd) + toLibraryClassesRoots(entityToAdd),
        entitySource = entitySource,
      ) {
        this.excludedRoots = arrayListOf()
      }

    val foundLibrary = builder.resolve(LibraryId(entityToAdd.displayName, tableId))
    if (foundLibrary != null) return foundLibrary

    return builder.addEntity(libraryEntity)
  }

  private fun toLibrarySourcesRoots(entityToAdd: Library): List<LibraryRoot> =
    entityToAdd.sourceJars.map {
      LibraryRoot(
        url = Library.formatJarString(it).toResolvedVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        type = LibraryRootTypeId.SOURCES,
      )
    }

  private fun toLibraryClassesRoots(entityToAdd: Library): List<LibraryRoot> =
    entityToAdd.classJars.ifEmpty { entityToAdd.iJars }.map {
      LibraryRoot(
        url = Library.formatJarString(it).toResolvedVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        type = LibraryRootTypeId.COMPILED,
      )
    }
}

internal fun calculateLibraryEntitySource(workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig): EntitySource =
  when {
    !JpsFeatureFlags.isJpsCompilationEnabled -> BspProjectEntitySource
    else ->
      LegacyBridgeJpsEntitySourceFactory.createEntitySourceForProjectLibrary(
        project = workspaceModelEntityUpdaterConfig.project,
        externalSource = workspaceModelEntityUpdaterConfig.project.bspProjectModelExternalSource,
      )
  }
