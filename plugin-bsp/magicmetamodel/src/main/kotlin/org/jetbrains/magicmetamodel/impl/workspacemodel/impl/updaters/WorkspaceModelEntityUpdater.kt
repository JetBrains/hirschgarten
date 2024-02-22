package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelEntity
import java.nio.file.Path

internal data class WorkspaceModelEntityUpdaterConfig(
  val workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  val projectBasePath: Path,
  val project: Project,
)

internal sealed interface WorkspaceModelEntityUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity>

internal interface WorkspaceModelEntityWithParentModuleUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity> :
  WorkspaceModelEntityUpdater<E, R> {
  fun addEntries(entriesToAdd: List<E>, parentModuleEntity: ModuleEntity): List<R> =
    entriesToAdd.map { addEntity(it, parentModuleEntity) }

  fun addEntity(entityToAdd: E, parentModuleEntity: ModuleEntity): R
}

internal interface WorkspaceModelEntityWithoutParentModuleUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity>
: WorkspaceModelEntityUpdater<E, R> {
  fun addEntries(entriesToAdd: List<E>): List<R> =
    entriesToAdd.map { addEntity(it) }

  fun addEntity(entityToAdd: E): R
}

internal interface WorkspaceModuleEntityRemover<in E> {
  fun removeEntity(entityToRemove: E)

  fun clear()
}
