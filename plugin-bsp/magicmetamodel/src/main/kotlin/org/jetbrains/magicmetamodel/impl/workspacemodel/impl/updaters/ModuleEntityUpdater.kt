package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.module.impl.ModuleManagerEx
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleCustomImlDataEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.customImlData
import com.intellij.platform.workspace.jps.entities.modifyEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.workspaceModel.ide.impl.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsConstants
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsPaths
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.IntermediateLibraryDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.IntermediateModuleDependency
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.workspacemodel.entities.BspEntitySource
import org.jetbrains.workspacemodel.entities.BspProjectDirectoriesEntity

internal class ModuleEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val defaultDependencies: List<ModuleDependencyItem> = ArrayList(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<GenericModuleInfo, ModuleEntity> {
  override fun addEntity(entityToAdd: GenericModuleInfo): ModuleEntity =
    addModuleEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd)

  private fun addModuleEntity(
    builder: MutableEntityStorage,
    entityToAdd: GenericModuleInfo,
  ): ModuleEntity {
    val modulesDependencies = entityToAdd.modulesDependencies.map { toModuleDependencyItemModuleDependency(it) }
    val associatesDependencies = entityToAdd.associates.map { toModuleDependencyItemModuleDependency(it) }
    val librariesDependencies =
      entityToAdd.librariesDependencies.map { toModuleDependencyItemLibraryDependency(it, entityToAdd.name) }
    val moduleEntity = builder.addEntity(
      ModuleEntity(
        name = entityToAdd.name,
        dependencies = defaultDependencies + modulesDependencies + associatesDependencies + librariesDependencies,
        entitySource = toEntitySource(entityToAdd),
      ) {
        this.type = entityToAdd.type
      },
    )
    val imlData = builder.addEntity(
      ModuleCustomImlDataEntity(
        customModuleOptions = entityToAdd.capabilities.asMap() + entityToAdd.languageIdsAsSingleEntryMap,
        entitySource = moduleEntity.entitySource,
      ) {
        this.rootManagerTagCustomData = null
        this.module = moduleEntity
      },
    )
    builder.modifyEntity(moduleEntity) {
      this.customImlData = imlData
    }
    return moduleEntity
  }

  private fun toEntitySource(entityToAdd: GenericModuleInfo): EntitySource = when {
    entityToAdd.isDummy -> BspDummyEntitySource
    entityToAdd.languageIds.any { it !in JpsConstants.SUPPORTED_LANGUAGES } -> BspEntitySource
    else -> LegacyBridgeJpsEntitySourceFactory.createEntitySourceForModule(
      project = workspaceModelEntityUpdaterConfig.project,
      baseModuleDir = JpsPaths.getJpsImlModulesPath(workspaceModelEntityUpdaterConfig.projectBasePath)
        .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
      externalSource = null,
      moduleFileName = entityToAdd.name + ModuleManagerEx.IML_EXTENSION
    )
  }

  private fun toModuleDependencyItemModuleDependency(
    intermediateModuleDependency: IntermediateModuleDependency,
  ): ModuleDependency =
    ModuleDependency(
      module = ModuleId(intermediateModuleDependency.moduleName),
      exported = true,
      scope = DependencyScope.COMPILE,
      productionOnTest = true,
    )
}

internal fun toModuleDependencyItemLibraryDependency(
  intermediateLibraryDependency: IntermediateLibraryDependency,
  moduleName: String,
): LibraryDependency {
  val libraryTableId = if (intermediateLibraryDependency.isProjectLevelLibrary)
    LibraryTableId.ProjectLibraryTableId else LibraryTableId.ModuleLibraryTableId(ModuleId(moduleName))
  return LibraryDependency(
    library = LibraryId(
      name = intermediateLibraryDependency.libraryName,
      tableId = libraryTableId,
    ),
    exported = true, // TODO https://youtrack.jetbrains.com/issue/BAZEL-632
    scope = DependencyScope.COMPILE,
  )
}

internal class WorkspaceModuleRemover(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModuleEntityRemover<ModuleName> {
  override fun removeEntity(entityToRemove: ModuleName) {
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-634
    val moduleToRemove =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.resolve(ModuleId(entityToRemove.name))!!

    workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.removeEntity(moduleToRemove)
  }

  override fun clear() {
    removeEntities(ModuleEntity::class.java)
    removeEntities(LibraryEntity::class.java)
    removeEntities(BspProjectDirectoriesEntity::class.java)
  }

  private fun <E : WorkspaceEntity> removeEntities(entityClass: Class<E>) {
    val allEntities = workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.entities(entityClass)
    allEntities.forEach { workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.removeEntity(it) }
  }
}
