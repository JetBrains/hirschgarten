package org.jetbrains.magicmetamodel.impl

import com.intellij.workspaceModel.ide.BuilderSnapshot
import com.intellij.workspaceModel.ide.StorageReplacement
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.storage.EntityChange
import com.intellij.workspaceModel.storage.EntityReference
import com.intellij.workspaceModel.storage.EntitySource
import com.intellij.workspaceModel.storage.ExternalEntityMapping
import com.intellij.workspaceModel.storage.ModifiableWorkspaceEntity
import com.intellij.workspaceModel.storage.MutableExternalEntityMapping
import com.intellij.workspaceModel.storage.PersistentEntityId
import com.intellij.workspaceModel.storage.WorkspaceEntity
import com.intellij.workspaceModel.storage.WorkspaceEntityStorage
import com.intellij.workspaceModel.storage.WorkspaceEntityStorageBuilder
import com.intellij.workspaceModel.storage.WorkspaceEntityStorageDiffBuilder
import com.intellij.workspaceModel.storage.WorkspaceEntityWithPersistentId
import com.intellij.workspaceModel.storage.url.MutableVirtualFileUrlIndex
import com.intellij.workspaceModel.storage.url.VirtualFileUrlIndex
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

// will be used in the future
@Suppress("UnusedPrivateClass")
private class WorkspaceEntityStorageBuilderTestMockImpl : WorkspaceEntityStorageBuilder {

  override val modificationCount by lazy {
    throw NotImplementedError("This is test mock implementation - method implementation not provided")
  }

  override fun <M : ModifiableWorkspaceEntity<T>, T : WorkspaceEntity> addEntity(
    clazz: Class<M>,
    source: EntitySource,
    initializer: M.() -> Unit,
  ): T {
    TODO("Not yet implemented")
  }

  override fun removeEntity(e: WorkspaceEntity) {
    TODO("Not yet implemented")
  }

  override fun addDiff(diff: WorkspaceEntityStorageDiffBuilder) =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <T : WorkspaceEntity> changeSource(e: T, newSource: EntitySource): T =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun collectChanges(original: WorkspaceEntityStorage): Map<Class<*>, List<EntityChange<*>>> =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <E : WorkspaceEntity> createReference(e: E): EntityReference<E> =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <E : WorkspaceEntity> entities(entityClass: Class<E>): Sequence<E> =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <E : WorkspaceEntity> entitiesAmount(entityClass: Class<E>): Int =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun entitiesBySource(
    sourceFilter: (EntitySource) -> Boolean,
  ): Map<EntitySource, Map<Class<out WorkspaceEntity>, List<WorkspaceEntity>>> =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <T> getExternalMapping(identifier: String): ExternalEntityMapping<T> =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <T> getMutableExternalMapping(identifier: String): MutableExternalEntityMapping<T> =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun getMutableVirtualFileUrlIndex(): MutableVirtualFileUrlIndex =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun getVirtualFileUrlIndex(): VirtualFileUrlIndex =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun isEmpty(): Boolean =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <M : ModifiableWorkspaceEntity<out T>, T : WorkspaceEntity> modifyEntity(
    clazz: Class<M>,
    e: T,
    change: M.() -> Unit,
  ): T =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <E : WorkspaceEntity, R : WorkspaceEntity> referrers(
    e: E,
    entityClass: KClass<R>,
    property: KProperty1<R, EntityReference<E>>,
  ): Sequence<R> =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <E : WorkspaceEntityWithPersistentId, R : WorkspaceEntity> referrers(
    id: PersistentEntityId<E>,
    entityClass: Class<R>,
  ): Sequence<R> =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun replaceBySource(sourceFilter: (EntitySource) -> Boolean, replaceWith: WorkspaceEntityStorage) =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <E : WorkspaceEntityWithPersistentId> resolve(id: PersistentEntityId<E>): E? =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun toStorage(): WorkspaceEntityStorage =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")
}

internal class WorkspaceModelTestMockImpl : WorkspaceModel {

  override val cache by lazy {
    throw NotImplementedError("This is test mock implementation - method implementation not provided")
  }
  override val entityStorage by lazy {
    throw NotImplementedError("This is test mock implementation - method implementation not provided")
  }

  override fun <R> updateProjectModel(updater: (WorkspaceEntityStorageBuilder) -> R): R {
    TODO("Not yet implemented")
  }

  override fun getBuilderSnapshot(): BuilderSnapshot =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun replaceProjectModel(replacement: StorageReplacement): Boolean =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")

  override fun <R> updateProjectModelSilent(updater: (WorkspaceEntityStorageBuilder) -> R): R =
    throw NotImplementedError("This is test mock implementation - method implementation not provided")
}
