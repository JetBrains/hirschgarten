package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.plugins.bsp.workspacemodel.entities.WorkspaceModelEntity
import java.nio.file.Path

data class WorkspaceModelEntityUpdaterConfig(
  val workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  val projectBasePath: Path,
  val project: Project,
)

internal sealed interface WorkspaceModelEntityUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity>

internal interface WorkspaceModelEntityWithParentModuleUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity> :
  WorkspaceModelEntityUpdater<E, R> {
  fun addEntities(entitiesToAdd: List<E>, parentModuleEntity: ModuleEntity): List<R> =
    entitiesToAdd.map { addEntity(it, parentModuleEntity) }

  fun addEntity(entityToAdd: E, parentModuleEntity: ModuleEntity): R
}

internal interface WorkspaceModelEntityWithoutParentModuleUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity> :
  WorkspaceModelEntityUpdater<E, R> {
  fun addEntities(entitiesToAdd: List<E>): List<R> = entitiesToAdd.map { addEntity(it) }

  fun addEntity(entityToAdd: E): R
}
