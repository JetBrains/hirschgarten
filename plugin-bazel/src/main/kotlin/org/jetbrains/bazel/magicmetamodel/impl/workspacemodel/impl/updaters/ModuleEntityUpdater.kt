package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.jps.entities.DependencyScope
import com.intellij.platform.workspace.jps.entities.LibraryDependency
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleDependency
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.SymbolicEntityId
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.util.containers.Interner
import com.intellij.workspaceModel.ide.legacyBridge.LegacyBridgeJpsEntitySourceFactory
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.jpsCompilation.utils.JpsConstants
import org.jetbrains.bazel.jpsCompilation.utils.JpsPaths
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.target.addLibraryModulePrefix
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.Library

private val dependencyInterner: Interner<ModuleDependencyItem> = Interner.createWeakInterner()
private val idInterner: Interner<SymbolicEntityId<*>> = Interner.createWeakInterner()

class ModuleEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val defaultDependencies: List<ModuleDependencyItem> = ArrayList(),
  private val libraries: Map<String, Library>,
  private val runtimeDependencies: List<String> = emptyList(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<GenericModuleInfo, ModuleEntity> {
  override suspend fun addEntity(entityToAdd: GenericModuleInfo): ModuleEntity =
    addModuleEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd)

  private fun addModuleEntity(builder: MutableEntityStorage, entityToAdd: GenericModuleInfo): ModuleEntity {
    val associatesDependencies = entityToAdd.associates.map { toModuleDependencyItemModuleDependency(it) }
    val dependenciesFromEntity =
      entityToAdd.dependencies.map { dependency ->
        val runtime = runtimeDependencies.contains(dependency)
        val libraryDependency = libraries[dependency]
        if (libraryDependency != null) {
          val exported = !libraryDependency.isLowPriority
          if (BazelFeatureFlags.isWrapLibrariesInsideModulesEnabled && !entityToAdd.isLibraryModule) {
            toModuleDependencyItemModuleDependency(dependency.addLibraryModulePrefix(), exported, runtime)
          } else {
            toLibraryDependency(dependency, exported = exported || entityToAdd.isLibraryModule, runtime)
          }
        } else {
          toModuleDependencyItemModuleDependency(dependency, runtime = runtime)
        }
      }

    val dependencies =
      defaultDependencies +
        dependenciesFromEntity +
        associatesDependencies

    val moduleEntityBuilder =
      ModuleEntity(
        name = entityToAdd.name,
        dependencies = dependencies,
        entitySource = toEntitySource(entityToAdd),
      ) {
        this.type = entityToAdd.type
      }

    return builder.addEntity(moduleEntityBuilder)
  }

  private fun toEntitySource(entityToAdd: GenericModuleInfo): EntitySource =
    when {
      entityToAdd.isDummy -> BazelDummyEntitySource
      !workspaceModelEntityUpdaterConfig.project.bazelJVMProjectSettings.enableBuildWithJps ||
        entityToAdd.kind.languageClasses.any { it !in JpsConstants.SUPPORTED_LANGUAGES } -> BazelModuleEntitySource(entityToAdd.name)

      else ->
        LegacyBridgeJpsEntitySourceFactory.getInstance(workspaceModelEntityUpdaterConfig.project).createEntitySourceForModule(
          JpsPaths
            .getJpsImlModulesPath(workspaceModelEntityUpdaterConfig.projectBasePath)
            .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
          BazelProjectModelExternalSource,
        )
    }

  private fun toModuleDependencyItemModuleDependency(moduleName: String, exported: Boolean = true, runtime: Boolean = false): ModuleDependency =
    dependencyInterner.intern(
      ModuleDependency(
        module = idInterner.intern(ModuleId(moduleName)) as ModuleId,
        exported = exported,
        scope = dependencyScope(runtime),
        productionOnTest = true,
      ),
    ) as ModuleDependency
}

internal fun toLibraryDependency(libraryName: String, exported: Boolean = true, runtime: Boolean = false): LibraryDependency =
  dependencyInterner.intern(
    LibraryDependency(
      library =
        idInterner.intern(
          LibraryId(
            name = libraryName,
            tableId = LibraryTableId.ProjectLibraryTableId, // treat all libraries as project-level libraries
          ),
        ) as LibraryId,
      exported = exported, // TODO https://youtrack.jetbrains.com/issue/BAZEL-632
      scope = dependencyScope(runtime),
    ),
  ) as LibraryDependency

private fun dependencyScope(runtime: Boolean): DependencyScope =
  if (runtime) DependencyScope.RUNTIME else DependencyScope.COMPILE
