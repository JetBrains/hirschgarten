package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.MutableEntityStorage
import com.intellij.workspaceModel.storage.WorkspaceEntity
import com.intellij.workspaceModel.storage.bridgeEntities.ModuleEntity
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import java.nio.file.Path

internal object DoNotSaveInDotIdeaDirEntitySource : EntitySource

internal abstract class WorkspaceModelEntity

internal data class WorkspaceModelEntityUpdaterConfig(
  val workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  val projectBasePath: Path,
)

internal sealed interface WorkspaceModelEntityUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity>

internal interface WorkspaceModelEntityWithParentModuleUpdater
<in E : WorkspaceModelEntity, out R : WorkspaceEntity> :
  WorkspaceModelEntityUpdater<E, R> {

  fun addEntries(entriesToAdd: List<E>, parentModuleEntity: ModuleEntity): List<R> =
    entriesToAdd.map { addEntity(it, parentModuleEntity) }

  fun addEntity(entityToAdd: E, parentModuleEntity: ModuleEntity): R
}

internal interface WorkspaceModelEntityWithoutParentModuleUpdater
<in E : WorkspaceModelEntity, out R : WorkspaceEntity> :
  WorkspaceModelEntityUpdater<E, R> {

  fun addEntries(entriesToAdd: List<E>): List<R> =
    entriesToAdd.map { addEntity(it) }

  fun addEntity(entityToAdd: E): R
}

internal interface WorkspaceModuleEntityRemover<in E> {

  fun removeEntity(entityToRemove: E)

  fun clear()
}
