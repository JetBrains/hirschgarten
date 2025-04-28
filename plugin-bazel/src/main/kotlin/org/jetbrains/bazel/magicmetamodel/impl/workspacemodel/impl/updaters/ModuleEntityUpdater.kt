package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

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
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.util.containers.Interner
import com.intellij.workspaceModel.ide.legacyBridge.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.bazel.jpsCompilation.utils.JpsConstants
import org.jetbrains.bazel.jpsCompilation.utils.JpsPaths
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.target.addLibraryModulePrefix
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BspModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.IntermediateLibraryDependency
import org.jetbrains.bazel.workspacemodel.entities.IntermediateModuleDependency

private val dependencyInterner: Interner<ModuleDependencyItem> = Interner.createWeakInterner()
private val idInterner: Interner<SymbolicEntityId<*>> = Interner.createWeakInterner()

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
        librariesDependencies.map { toLibraryDependency(it) } +
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
        customModuleOptions = emptyMap(),
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
      !workspaceModelEntityUpdaterConfig.project.bazelProjectSettings.enableBuildWithJps ||
        entityToAdd.kind.languageClasses.any { it !in JpsConstants.SUPPORTED_LANGUAGES } -> BspModuleEntitySource(entityToAdd.name)

      else ->
        LegacyBridgeJpsEntitySourceFactory.getInstance(workspaceModelEntityUpdaterConfig.project).createEntitySourceForModule(
          JpsPaths
            .getJpsImlModulesPath(workspaceModelEntityUpdaterConfig.projectBasePath)
            .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
          BazelProjectModelExternalSource,
        )
    }

  private fun toModuleDependencyItemModuleDependency(intermediateModuleDependency: IntermediateModuleDependency): ModuleDependency =
    dependencyInterner.intern(
      ModuleDependency(
        module = idInterner.intern(ModuleId(intermediateModuleDependency.moduleName)) as ModuleId,
        exported = true,
        scope = DependencyScope.COMPILE,
        productionOnTest = true,
      ),
    ) as ModuleDependency
}

internal fun toLibraryDependency(intermediateLibraryDependency: IntermediateLibraryDependency): LibraryDependency =
  dependencyInterner.intern(
    LibraryDependency(
      library =
        idInterner.intern(
          LibraryId(
            name = intermediateLibraryDependency.libraryName,
            tableId = LibraryTableId.ProjectLibraryTableId, // treat all libraries as project-level libraries
          ),
        ) as LibraryId,
      exported = true, // TODO https://youtrack.jetbrains.com/issue/BAZEL-632
      scope = DependencyScope.COMPILE,
    ),
  ) as LibraryDependency
