package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.platform.workspace.storage.WorkspaceEntity
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GoModule

internal class GoModuleUpdater(
  private val workspaceModelEntityUpdaterConfig: WorkspaceModelEntityUpdaterConfig,
) : WorkspaceModelEntityWithoutParentModuleUpdater<GoModule, WorkspaceEntity> {
  override fun addEntity(entityToAdd: GoModule): WorkspaceEntity {
    val moduleEntityUpdater = ModuleEntityUpdater(workspaceModelEntityUpdaterConfig)
    val moduleEntity = moduleEntityUpdater.addEntity(entityToAdd.module)

    val sourceEntityUpdater = SourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    sourceEntityUpdater.addEntries(entityToAdd.sourceRoots, moduleEntity)

    val goResourceEntityUpdater = GoResourceEntityUpdater(workspaceModelEntityUpdaterConfig)
    goResourceEntityUpdater.addEntries(entityToAdd.resourceRoots, moduleEntity)

    if (!goEntitiesExtensionExists()) {
      error("Go entities extension does not exist.")
    }
    val goEntitiesExtension = goEntitiesExtension()!!

    val goModuleEntities = goEntitiesExtension.prepareAllEntitiesForGoModule(
      entityToAdd,
      moduleEntity,
      workspaceModelEntityUpdaterConfig.virtualFileUrlManager,
    )

    goModuleEntities.goDependenciesWorkspaceEntity.forEach {
      workspaceModelEntityUpdaterConfig.workspaceEntityStorageBuilder.addEntity(it)
    }
    return goModuleEntities.goModuleWorkspaceEntity
  }
}
