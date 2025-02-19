package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

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
import com.intellij.workspaceModel.ide.legacyBridge.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.bazel.extensionPoints.projectModelExternalSource
import org.jetbrains.bazel.projectAware.BspWorkspace
import org.jetbrains.bazel.target.addLibraryModulePrefix
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BspModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.IntermediateLibraryDependency
import org.jetbrains.bazel.workspacemodel.entities.IntermediateModuleDependency
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsConstants
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsFeatureFlags
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JpsPaths

internal class ModuleEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val defaultDependencies: List<ModuleDependencyItem> = ArrayList(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<GenericModuleInfo, ModuleEntity> {
  override suspend fun addEntity(entityToAdd: GenericModuleInfo): ModuleEntity =
    addModuleEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd)

  private fun addModuleEntity(builder: MutableEntityStorage, entityToAdd: GenericModuleInfo): ModuleEntity {
    val associatesDependencies = entityToAdd.associates.map { toModuleDependencyItemModuleDependency(it) }
    val (libraryModulesDependencies, librariesDependencies) =
      entityToAdd.librariesDependencies.partition {
        !entityToAdd.isLibraryModule &&
          workspaceModelEntityUpdaterConfig.project.targetUtils.isLibraryModule(it.libraryName)
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
          workspaceModelEntityUpdaterConfig.project.projectModelExternalSource,
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
