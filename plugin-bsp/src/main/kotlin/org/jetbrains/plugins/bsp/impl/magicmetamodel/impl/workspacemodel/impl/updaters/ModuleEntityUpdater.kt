package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.module.impl.ModuleManagerEx
import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleCustomImlDataEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.customImlData
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.workspaceModel.ide.impl.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsConstants
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsPaths
import org.jetbrains.plugins.bsp.impl.projectAware.BspWorkspace
import org.jetbrains.plugins.bsp.impl.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.plugins.bsp.workspacemodel.entities.BspEntitySource
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
    val associatesDependencies = entityToAdd.associates.map { toModuleDependencyItemModuleDependency(it) }
    val (libraryModulesDependencies, librariesDependencies) =
      entityToAdd.librariesDependencies.partition {
        !entityToAdd.isLibraryModule &&
          workspaceModelEntityUpdaterConfig.project.temporaryTargetUtils.isLibraryModule(it.libraryName)
      }
    val modulesDependencies =
      (entityToAdd.modulesDependencies + libraryModulesDependencies.toLibraryModuleDependencies()).map {
        toModuleDependencyItemModuleDependency(it)
      }
    val dependencies =
      defaultDependencies +
        modulesDependencies +
        librariesDependencies.map { toLibraryDependency(it, workspaceModelEntityUpdaterConfig.project) } +
        associatesDependencies

    val moduleEntityBuilder =
      ModuleEntity(
        name = entityToAdd.name,
        dependencies = dependencies,
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

  private fun List<IntermediateLibraryDependency>.toLibraryModuleDependencies() = this.map { IntermediateModuleDependency(it.libraryName) }

  private fun toEntitySource(entityToAdd: GenericModuleInfo): EntitySource =
    when {
      entityToAdd.isDummy -> BspDummyEntitySource
      !JpsFeatureFlags.isJpsCompilationEnabled ||
        entityToAdd.languageIds.any { it !in JpsConstants.SUPPORTED_LANGUAGES } -> BspEntitySource

      else ->
        LegacyBridgeJpsEntitySourceFactory.createEntitySourceForModule(
          project = workspaceModelEntityUpdaterConfig.project,
          baseModuleDir =
            JpsPaths
              .getJpsImlModulesPath(workspaceModelEntityUpdaterConfig.projectBasePath)
              .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
          externalSource = null,
          moduleFileName = entityToAdd.name + ModuleManagerEx.IML_EXTENSION,
        )
    }

  private fun toModuleDependencyItemModuleDependency(
    intermediateModuleDependency: IntermediateModuleDependency,
    project: Project = workspaceModelEntityUpdaterConfig.project,
  ): ModuleDependency =
    BspWorkspace.getInstance(project).interner.getOrPut(
      ModuleDependency(
        module = BspWorkspace.getInstance(project).interner.getOrPut(ModuleId(intermediateModuleDependency.moduleName)),
        exported = true,
        scope = DependencyScope.COMPILE,
        productionOnTest = true,
      ),
    )
}

internal fun toLibraryDependency(intermediateLibraryDependency: IntermediateLibraryDependency, project: Project): LibraryDependency =
  BspWorkspace.getInstance(project).interner.getOrPut(
    LibraryDependency(
      library =
        BspWorkspace.getInstance(project).interner.getOrPut(
          LibraryId(
            name = intermediateLibraryDependency.libraryName,
            tableId = LibraryTableId.ProjectLibraryTableId, // treat all libraries as project-level libraries
          ),
        ),
      exported = true, // TODO https://youtrack.jetbrains.com/issue/BAZEL-632
      scope = DependencyScope.COMPILE,
    ),
  )
