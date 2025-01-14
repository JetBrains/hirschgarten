package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.ModuleCustomImlDataEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.customImlData
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.workspaceModel.ide.legacyBridge.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsConstants
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsPaths
import org.jetbrains.plugins.bsp.extensionPoints.bspProjectModelExternalSource
import org.jetbrains.plugins.bsp.target.addLibraryModulePrefix
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspModuleEntitySource
import org.jetbrains.plugins.bsp.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateLibraryDependency
import org.jetbrains.plugins.bsp.workspacemodel.entities.IntermediateModuleDependency

internal class ModuleEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val defaultDependencies: List<ModuleDependencyItem> = ArrayList(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<GenericModuleInfo, ModuleEntity> {
  override fun addEntity(entityToAdd: GenericModuleInfo): ModuleEntity =
    addModuleEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd)

  private fun addModuleEntity(builder: MutableEntityStorage, entityToAdd: GenericModuleInfo): ModuleEntity {
    val associatesDependencies = entityToAdd.associates.map { }
    val (libraryModulesDependencies, librariesDependencies) =
      entityToAdd.librariesDependencies.partition {
        !entityToAdd.isLibraryModule &&
          workspaceModelEntityUpdaterConfig.project.temporaryTargetUtils.isLibraryModule(it.libraryName)
      }
    val modulesDependencies =
      (entityToAdd.modulesDependencies + libraryModulesDependencies.toLibraryModuleDependencies()).map {
      }
    val dependencies =
      defaultDependencies +
        modulesDependencies +
        librariesDependencies.map { } +
        associatesDependencies

    val moduleEntityBuilder =
      ModuleEntity(
        name = entityToAdd.name,
        dependencies = listOf(),
        entitySource = toEntitySource(entityToAdd),
      ) {
        this.type = entityToAdd.type
      }

    val moduleEntity = builder.addEntity(moduleEntityBuilder)

    val imlData =
      ModuleCustomImlDataEntity(
        customModuleOptions = entityToAdd.capabilities.asMap() + entityToAdd.languageIdsAsSingleEntryMap,
        entitySource = moduleEntity.entitySource,
      ) {
        this.rootManagerTagCustomData = null
        this.module = moduleEntityBuilder
      }

    // TODO: use a separate entity instead of imlData
    return builder.modifyModuleEntity(moduleEntity) {
      this.customImlData = imlData
    }
  }

  private fun List<IntermediateLibraryDependency>.toLibraryModuleDependencies() =
    this.map {
      IntermediateModuleDependency(it.libraryName.addLibraryModulePrefix())
    }

  private fun toEntitySource(entityToAdd: GenericModuleInfo): EntitySource =
    when {
      entityToAdd.isDummy -> BspDummyEntitySource
      !JpsFeatureFlags.isJpsCompilationEnabled ||
        entityToAdd.languageIds.any { it !in JpsConstants.SUPPORTED_LANGUAGES } -> BspModuleEntitySource(entityToAdd.name)

      else ->
        LegacyBridgeJpsEntitySourceFactory.getInstance(workspaceModelEntityUpdaterConfig.project).createEntitySourceForModule(
          JpsPaths
            .getJpsImlModulesPath(workspaceModelEntityUpdaterConfig.projectBasePath)
            .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
          workspaceModelEntityUpdaterConfig.project.bspProjectModelExternalSource,
        )
    }
}
