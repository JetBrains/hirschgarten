package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.configurationStore.serialize
import com.intellij.externalSystem.ImportedLibraryProperties
import com.intellij.externalSystem.ImportedLibraryType
import com.intellij.java.library.MavenCoordinates
import com.intellij.openapi.util.JDOMUtil
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryPropertiesEntity
import com.intellij.platform.workspace.jps.entities.LibraryRoot
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.LibraryTypeId
import com.intellij.platform.workspace.jps.entities.libraryProperties
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.workspaceModel.ide.legacyBridge.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.jps.model.serialization.library.JpsLibraryTableSerializer

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
  override suspend fun addEntity(entityToAdd: Library): LibraryEntity =
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
    val foundLibrary = builder.resolve(LibraryId(entityToAdd.displayName, tableId))
    if (foundLibrary != null) return foundLibrary

    val libraryEntity =
      LibraryEntity(
        name = entityToAdd.displayName,
        tableId = tableId,
        roots = toLibrarySourcesRoots(entityToAdd) + toLibraryClassesRoots(entityToAdd),
        entitySource = entitySource,
      ) {
        this.excludedRoots = arrayListOf()
        this.typeId = LibraryTypeId(ImportedLibraryType.IMPORTED_LIBRARY_KIND.kindId)
        this.libraryProperties =
          LibraryPropertiesEntity(entitySource) {
            propertiesXmlTag = toLibraryPropertiesXml(entityToAdd)
          }
      }

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

  private fun toLibraryPropertiesXml(entityToAdd: Library): String? {
    val mavenCoordinates = entityToAdd.mavenCoordinates ?: return null
    val libPropertiesElement =
      serialize(
        ImportedLibraryProperties(
          MavenCoordinates(
            mavenCoordinates.groupId,
            mavenCoordinates.artifactId,
            mavenCoordinates.version,
          ),
        ).state,
      ) ?: return null
    libPropertiesElement.name = JpsLibraryTableSerializer.PROPERTIES_TAG
    return JDOMUtil.writeElement(libPropertiesElement)
  }
}

internal fun calculateLibraryEntitySource(workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig): EntitySource =
  when {
    !workspaceModelEntityUpdaterConfig.project.bazelProjectSettings.enableBuildWithJps -> BazelProjectEntitySource
    else ->
      LegacyBridgeJpsEntitySourceFactory
        .getInstance(workspaceModelEntityUpdaterConfig.project)
        .createEntitySourceForProjectLibrary(
          BazelProjectModelExternalSource,
        )
  }
