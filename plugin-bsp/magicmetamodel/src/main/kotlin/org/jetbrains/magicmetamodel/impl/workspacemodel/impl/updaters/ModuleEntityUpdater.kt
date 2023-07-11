package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.platform.workspace.jps.entities.LibraryTableId
import com.intellij.platform.workspace.jps.entities.ModuleCustomImlDataEntity
import com.intellij.platform.workspace.jps.entities.ModuleDependencyItem
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.ModuleId
import com.intellij.platform.workspace.jps.entities.modifyEntity
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName

internal data class ModuleDependency(
  val moduleName: String,
) : WorkspaceModelEntity()

internal data class LibraryDependency(
  val libraryName: String,
  val isProjectLevelLibrary: Boolean = false
) : WorkspaceModelEntity()

internal data class Module(
  val name: String,
  val type: String,
  val modulesDependencies: List<ModuleDependency>,
  val librariesDependencies: List<LibraryDependency>,
  val capabilities: ModuleCapabilities = ModuleCapabilities(),
  val languageIds: List<String> = listOf(),
  val associates: List<ModuleDependency> = listOf(),
) : WorkspaceModelEntity()

internal class ModuleCapabilities(private val map: Map<String, String> = mapOf()) :
  Map<String, String> by map {
  val canRun: Boolean
    get() = map[KEYS.CAN_RUN.name].toBoolean()
  val canDebug: Boolean
    get() = map[KEYS.CAN_DEBUG.name].toBoolean()
  val canTest: Boolean
    get() = map[KEYS.CAN_TEST.name].toBoolean()
  val canCompile: Boolean
    get() = map[KEYS.CAN_COMPILE.name].toBoolean()

  internal constructor(canRun: Boolean, canTest: Boolean, canCompile: Boolean, canDebug: Boolean) : this(
    mapOf(
      KEYS.CAN_RUN.name to canRun.toString(),
      KEYS.CAN_DEBUG.name to canDebug.toString(),
      KEYS.CAN_TEST.name to canTest.toString(),
      KEYS.CAN_COMPILE.name to canCompile.toString(),
    )
  )

  private enum class KEYS {
    CAN_RUN,
    CAN_DEBUG,
    CAN_TEST,
    CAN_COMPILE,
  }
}

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
    val modulesDependencies = entityToAdd.modulesDependencies.map { toModuleDependencyItemModuleDependency(it) }
    val associatesDependencies = entityToAdd.associates.map { toModuleDependencyItemModuleDependency(it) }
    val librariesDependencies =
      entityToAdd.librariesDependencies.map { toModuleDependencyItemLibraryDependency(it, entityToAdd.name) }
    val moduleEntity = builder.addEntity(
      ModuleEntity(
        name = entityToAdd.name,
        dependencies = modulesDependencies + associatesDependencies + librariesDependencies + defaultDependencies,
        entitySource = BspEntitySource
      ) {
        this.type = entityToAdd.type
      }
    )
    val imlData = builder.addEntity(
      ModuleCustomImlDataEntity(
        customModuleOptions = HashMap(entityToAdd.capabilities),
        entitySource = BspEntitySource
      ) {
        this.rootManagerTagCustomData = null
        this.module = moduleEntity
      }
    )
    builder.modifyEntity(moduleEntity) {
      this.customImlData = imlData
    }
    return moduleEntity
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
  ): ModuleDependencyItem.Exportable.LibraryDependency {
    val libraryTableId = if (libraryDependency.isProjectLevelLibrary)
      LibraryTableId.ProjectLibraryTableId else LibraryTableId.ModuleLibraryTableId(ModuleId(moduleName))
    return ModuleDependencyItem.Exportable.LibraryDependency(
            library = LibraryId(
                    name = libraryDependency.libraryName,
                    tableId = libraryTableId,
            ),
            exported = false,
            scope = ModuleDependencyItem.DependencyScope.COMPILE,
    )
  }
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

    allModules.forEach { workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.removeEntity(it) }

    val allLibraries =
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.entities(LibraryEntity::class.java)
    allLibraries.forEach { workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.removeEntity(it) }
  }
}
