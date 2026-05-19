package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.roots.DependencyScope
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
import com.intellij.util.containers.Interner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelModuleExtensionEntity
import org.jetbrains.bazel.workspacemodel.entities.GenericModuleInfo
import org.jetbrains.bazel.workspacemodel.entities.Library
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabel
import org.jetbrains.bazel.workspacemodel.entities.WorkspaceModelTargetLabelList
import org.jetbrains.bazel.workspacemodel.entities.bazelModuleExtension
import com.intellij.platform.workspace.jps.entities.DependencyScope as EntitiesDependencyScope

private val dependencyInterner: Interner<ModuleDependencyItem> = Interner.createWeakInterner()
private val idInterner: Interner<SymbolicEntityId<*>> = Interner.createWeakInterner()

@ApiStatus.Internal
class ModuleEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val defaultDependencies: List<ModuleDependencyItem> = ArrayList(),
  private val modules: Map<String, Module>,
  private val libraries: Map<String, Library>,
) : WorkspaceModelEntityWithoutParentModuleUpdater<GenericModuleInfo, ModuleEntity> {
  override suspend fun addEntity(entityToAdd: GenericModuleInfo): ModuleEntity =
    addModuleEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd)

  private fun addModuleEntity(builder: MutableEntityStorage, entityToAdd: GenericModuleInfo): ModuleEntity {
    val associatesDependencies = entityToAdd.associates.map { toModuleDependencyItemModuleDependency(it, exported = true) }
    val dependenciesFromEntity =
      entityToAdd.dependencies.mapNotNull { dependency ->
        // Libraries may have the same name as its container module.
        // Prefer module dependency and use the library only inside the containing module to prevent dependency loop

        val moduleDependency = modules[dependency.id]?.takeIf { entityToAdd.name != dependency.id }
        if (moduleDependency != null) {
          return@mapNotNull toModuleDependencyItemModuleDependency(
            dependency.id,
            exported = dependency.exported,
            scope = dependency.scope,
          )
        }

        val libraryDependency = libraries[dependency.id]
        if (libraryDependency != null) {
          return@mapNotNull toLibraryDependency(
            dependency.id,
            exported = dependency.exported,
            scope = dependency.scope,
          )
        }

        null
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
        this.bazelModuleExtension = BazelModuleExtensionEntity(
          label = WorkspaceModelTargetLabel(entityToAdd.label),
          strictDependencies =
            WorkspaceModelTargetLabelList(
              entityToAdd.strictDependenciesCheck,
              entityToAdd.strictDependencies.map { it.toString() },
            ),
          entitySource = toEntitySource(entityToAdd),
        )
      }

    return builder.addEntity(moduleEntityBuilder)
  }

  private fun toEntitySource(entityToAdd: GenericModuleInfo): EntitySource =
    when {
      entityToAdd.kind.isJvmTarget() -> BazelModuleEntitySource(entityToAdd.name)
      else -> BazelDummyEntitySource
    }

  private fun toModuleDependencyItemModuleDependency(
    moduleName: String,
    exported: Boolean,
    scope: DependencyScope = DependencyScope.COMPILE,
  ): ModuleDependency =
    dependencyInterner.intern(
      ModuleDependency(
        module = idInterner.intern(ModuleId(moduleName)) as ModuleId,
        exported = exported,
        scope = scope.toEntityDependencyScope(),
        productionOnTest = true,
      ),
    ) as ModuleDependency
}

internal fun toLibraryDependency(libraryName: String, exported: Boolean, scope: DependencyScope = DependencyScope.COMPILE): LibraryDependency =
  dependencyInterner.intern(
    LibraryDependency(
      library =
        idInterner.intern(
          LibraryId(
            name = libraryName,
            tableId = LibraryTableId.ProjectLibraryTableId, // treat all libraries as project-level libraries
          ),
        ) as LibraryId,
      exported = exported,
      scope = scope.toEntityDependencyScope(),
    ),
  ) as LibraryDependency

private fun com.intellij.openapi.roots.DependencyScope.toEntityDependencyScope(): EntitiesDependencyScope = when (this) {
  com.intellij.openapi.roots.DependencyScope.COMPILE -> EntitiesDependencyScope.COMPILE
  com.intellij.openapi.roots.DependencyScope.RUNTIME -> EntitiesDependencyScope.RUNTIME
  com.intellij.openapi.roots.DependencyScope.PROVIDED -> EntitiesDependencyScope.PROVIDED
  com.intellij.openapi.roots.DependencyScope.TEST -> EntitiesDependencyScope.TEST
}
