package org.jetbrains.bazel.sync_new.lang.store

/**
 * Resource management system for incremental entity storage and dependency tracking.
 */
interface IncrementalEntityStore<R : IncrementalResourceId, E : IncrementalEntity> {
  /**
   * Get or create an entity with unique resource id,
   * in case when resource id is already taken return the existing entity
   */
  fun createEntity(resourceId: R, creator: IncrementalEntityCreator<E>): E

  /**
   * Modify existing entity with given resource id, return the modified entity
   */
  fun modifyEntity(resourceId: R, modifier: (E) -> E): E?

  /**
   * Remove entity with given resource id
   */
  fun removeEntity(resourceId: R): E?

  /**
   * Get existing entity with given resource id, otherwise null
   */
  fun getEntity(resourceId: R): E?

  /**
   * Add directed resource dependency
   */
  fun addDependency(from: R, to: R)

  /**
   * Remove directed resource dependency
   */
  fun removeDependency(from: R, to: R)

  /**
   * Get all dependants of the given resource id
   */
  fun getTransitiveDependants(resourceId: R): Sequence<R>

  /**
   * Get all direct referrers of the given resource id
   */
  fun getDirectReferrers(resourceId: R): Sequence<R>

  /**
   * Get all entities lazily
   */
  fun getAllEntities(): Sequence<E>

  /**
   * Clear all entities and dependencies
   */
  fun clear()
}

inline fun <reified U : E, R : IncrementalResourceId, E : IncrementalEntity> IncrementalEntityStore<R, E>.modifyEntityTyped(
  resourceId: R,
  crossinline modifier: (U) -> U,
): E? = modifyEntity(resourceId) {
  val entity = it as? U ?: return@modifyEntity it
  return@modifyEntity modifier(entity)
}
