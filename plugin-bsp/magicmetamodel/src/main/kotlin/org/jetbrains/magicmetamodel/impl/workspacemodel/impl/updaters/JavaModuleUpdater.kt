package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleDependencyItem
import com.intellij.workspaceModel.storage.bridgeEntities.api.ModuleEntity

internal data class JavaModule(
  val module: Module,
  val baseDirContentRoot: ContentRoot,
  val sourceRoots: List<JavaSourceRoot>,
  val resourceRoots: List<JavaResourceRoot>,
  val libraries: List<Library>,
//  val sdk: ModuleDependencyItem.SdkDependency,
) : WorkspaceModelEntity()

internal class JavaModuleWithSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig, defaultDependencies)
    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    val libraryEntityUpdater = LibraryEntityUpdater(workspaceModelEntityUpdaterConfig)
    libraryEntityUpdater.addEntries(entityToAdd.libraries, moduleEntity)

    val javaSourceEntityUpdater = JavaSourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    javaSourceEntityUpdater.addEntries(entityToAdd.sourceRoots, moduleEntity)

    val javaResourceEntityUpdater = JavaResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    javaResourceEntityUpdater.addEntries(entityToAdd.resourceRoots, moduleEntity)

    return moduleEntity
  }

  private companion object {
    val defaultDependencies = listOf(
      ModuleDependencyItem.SdkDependency("11", "JavaSDK"),
      ModuleDependencyItem.ModuleSourceDependency,
    )
  }
}

internal class JavaModuleWithoutSourcesUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig)
    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    val contentRootEntityUpdater = ContentRootEntityUpdater(workspaceModelEntityUpdaterConfig)
    contentRootEntityUpdater.addEntity(entityToAdd.baseDirContentRoot, moduleEntity)

    return moduleEntity
  }
}

internal class JavaModuleUpdater(
  workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<JavaModule, ModuleEntity> {

  private val javaModuleWithSourcesUpdater = JavaModuleWithSourcesUpdater(workspaceModelEntityUpdaterConfig)
  private val javaModuleWithoutSourcesUpdater = JavaModuleWithoutSourcesUpdater(workspaceModelEntityUpdaterConfig)

  override fun addEntity(entityToAdd: JavaModule): ModuleEntity =
    when (Pair(entityToAdd.sourceRoots.size, entityToAdd.resourceRoots.size)) {
      Pair(0, 0) -> javaModuleWithoutSourcesUpdater.addEntity(entityToAdd)
      else -> javaModuleWithSourcesUpdater.addEntity(entityToAdd)
    }
}
