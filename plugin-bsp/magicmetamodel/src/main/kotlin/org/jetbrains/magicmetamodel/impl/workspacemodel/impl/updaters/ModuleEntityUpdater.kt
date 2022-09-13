package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.bridgeEntities.addJavaModuleSettingsEntity
import com.intellij.workspaceModel.storage.bridgeEntities.addModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryId
import com.intellij.workspaceModel.storage.bridgeEntities.api.LibraryTableId
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleEntity
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleId
import com.intellij.workspaceModel.storage.impl.url.toVirtualFileUrl
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import java.net.URI
import kotlin.io.path.toPath

internal data class ModuleDependency(
  val moduleName: String,
) : WorkspaceModelEntity()

internal data class LibraryDependency(
  val libraryName: String,
) : WorkspaceModelEntity()

internal data class Module(
  val name: String,
  val type: String,
  val modulesDependencies: List<ModuleDependency>,
  val librariesDependencies: List<LibraryDependency>,
) : WorkspaceModelEntity()

internal class ModuleEntityUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
  private val defaultDependencies: List<ModuleDependencyItem> = ArrayList(),
) : WorkspaceModelEntityWithoutParentModuleUpdater<Module, ModuleEntity> {

  override fun addEntity(entityToAdd: Module): ModuleEntity =
    addModuleEntity(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder, entityToAdd)

  private fun addModuleEntity(
    builder: MutableEntityStorage,
    entityToAdd: Module,
  ): ModuleEntity {
    val modulesDependencies = entityToAdd.modulesDependencies.map(this::toModuleDependencyItemModuleDependency)
    val librariesDependencies =
      entityToAdd.librariesDependencies.map { toModuleDependencyItemLibraryDependency(it, entityToAdd.name) }


    return builder.addModuleEntity(
      name = entityToAdd.name,
      dependencies = modulesDependencies + librariesDependencies + defaultDependencies,
      source = DoNotSaveInDotIdeaDirEntitySource,
      type = entityToAdd.type
    ).also {
      builder.addJavaModuleSettingsEntity(
        inheritedCompilerOutput = false,
        excludeOutput = true,
        compilerOutput = URI("file:///private/var/tmp/_bazel_marcin.abramowicz/c736a3a8b7ef9068ff3ca29ae864a2cb/execroot/bazel_bsp/external/maven/v1/https/repo.maven.apache.org/maven2/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar").toPath()
          .toVirtualFileUrl(workspaceModelEntityUpdaterConfig.virtualFileUrlManager),
        compilerOutputForTests = null,
        languageLevelId = null,
        module = it,
        source = DoNotSaveInDotIdeaDirEntitySource,
      )
    }
  }

  private fun toModuleDependencyItemModuleDependency(
    moduleDependency: ModuleDependency
  ): ModuleDependencyItem.Exportable.ModuleDependency =
    ModuleDependencyItem.Exportable.ModuleDependency(
      module = ModuleId(moduleDependency.moduleName),
      exported = true,
      scope = ModuleDependencyItem.DependencyScope.COMPILE,
      productionOnTest = true,
    )

  private fun toModuleDependencyItemLibraryDependency(
    libraryDependency: LibraryDependency,
    moduleName: String
  ): ModuleDependencyItem.Exportable.LibraryDependency =
    ModuleDependencyItem.Exportable.LibraryDependency(
      library = LibraryId(
        name = libraryDependency.libraryName,
        tableId = LibraryTableId.ModuleLibraryTableId(ModuleId(moduleName)),
      ),
      exported = false,
      scope = ModuleDependencyItem.DependencyScope.COMPILE,
    )
}

// TODO TEST TEST TEST TEST TEST !!11!1!
internal class WorkspaceModuleRemover(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModuleEntityRemover<ModuleName> {

  override fun removeEntity(entityToRemove: ModuleName) {
    // TODO null
    val moduleToRemove =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.resolve(ModuleId(entityToRemove.name))!!

    workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.removeEntity(moduleToRemove)
  }

  override fun clear() {
    val allModules =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.entities(ModuleEntity::class.java)

    allModules.forEach(workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder::removeEntity)
  }
}
