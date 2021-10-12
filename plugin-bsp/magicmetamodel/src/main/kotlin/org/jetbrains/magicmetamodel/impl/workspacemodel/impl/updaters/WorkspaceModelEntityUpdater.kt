package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.impl.WorkspaceEntityBase
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager

internal abstract class WorkspaceModelEntity

internal data class WorkspaceModelEntityUpdaterConfig(
  val workspaceModel: WorkspaceModel,
  val virtualFileUrlManager: VirtualFileUrlManager,
  val projectConfigSource: EntitySource,
)

internal sealed interface WorkspaceModelEntityUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntityBase>

internal interface WorkspaceModelEntityWithParentModuleUpdater
<in E : WorkspaceModelEntity, out R : WorkspaceEntityBase> :
  WorkspaceModelEntityUpdater<E, R> {

  fun addEntries(entriesToAdd: List<E>, parentModuleEntity: ModuleEntity): List<R> =
    entriesToAdd.map { addEntity(it, parentModuleEntity) }

  fun addEntity(entityToAdd: E, parentModuleEntity: ModuleEntity): R
}

internal interface WorkspaceModelEntityWithoutParentModuleUpdater
<in E : WorkspaceModelEntity, out R : WorkspaceEntityBase> :
  WorkspaceModelEntityUpdater<E, R> {

  fun addEntries(entriesToAdd: List<E>): List<R> =
    entriesToAdd.map { addEntity(it) }

  fun addEntity(entityToAdd: E): R
}
