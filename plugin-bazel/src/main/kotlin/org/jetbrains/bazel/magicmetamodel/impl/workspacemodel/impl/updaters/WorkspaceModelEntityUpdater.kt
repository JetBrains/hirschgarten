package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.WorkspaceEntity
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.WorkspaceModelEntity
import java.nio.file.Path

data class WorkspaceModelEntityUpdaterConfig(
  val workspaceEntityStorageBuilder: MutableEntityStorage,
  val virtualFileUrlManager: VirtualFileUrlManager,
  val projectBasePath: Path,
  val project: Project,
)

sealed interface WorkspaceModelEntityUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity>

interface WorkspaceModelEntityWithParentModuleUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity> :
  WorkspaceModelEntityUpdater<E, R> {
  suspend fun addEntities(entitiesToAdd: List<E>, parentModuleEntity: ModuleEntity): List<R> =
    entitiesToAdd.map { addEntity(it, parentModuleEntity) }

  suspend fun addEntity(entityToAdd: E, parentModuleEntity: ModuleEntity): R
}

interface WorkspaceModelEntityWithoutParentModuleUpdater<in E : WorkspaceModelEntity, out R : WorkspaceEntity> :
  WorkspaceModelEntityUpdater<E, R> {
  suspend fun addEntities(entitiesToAdd: List<E>): List<R> = entitiesToAdd.mapNotNull { addEntity(it) }

  suspend fun addEntity(entityToAdd: E): R?
}
