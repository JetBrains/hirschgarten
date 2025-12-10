package org.jetbrains.bazel.sync_new.lang.store


object IncrementalEntityRemovalPropagator {
  fun <R : IncrementalResourceId, E : IncrementalEntity> remove(store: IncrementalEntityStore<R, E>, removed: Iterable<R>) {
    val toRemove = mutableSetOf<R>()
    for (removed in removed) {
      toRemove.add(removed)
      toRemove += store.getTransitiveDependants(removed)
    }
    val removeQueue = mutableListOf<R>()
    for (removed in removed) {
      for (dependency in store.getTransitiveDependants(removed)) {
        val referrers = store.getDirectReferrers(dependency).toList()
        val canBeRemoved = referrers.isEmpty()
          || referrers.all { it in toRemove }
        if (canBeRemoved) {
          removeQueue += dependency
        }
      }
    }
    removeQueue.forEach { store.removeEntity(it) }
  }
}
