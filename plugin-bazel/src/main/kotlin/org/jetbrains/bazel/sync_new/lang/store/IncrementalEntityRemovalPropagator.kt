package org.jetbrains.bazel.sync_new.lang.store


object IncrementalEntityRemovalPropagator {
  fun <R : IncrementalResourceId, E : IncrementalEntity> remove(store: IncrementalEntityStore<R, E>, removed: Iterable<R>) {
    val toRemove = hashSetOf<R>()
    toRemove += removed
    for (removed in removed) {
      toRemove += store.getTransitiveDependants(removed)
    }
    val removeQueue = LinkedHashSet<R>()
    removeQueue += removed
    for (candidate in toRemove) {
      val referrers = store.getDirectReferrers(candidate).toList()
      val canBeRemoved = referrers.isEmpty()
                         || referrers.all { it in toRemove }
      if (canBeRemoved) {
        removeQueue += candidate
      }
    }
    for (it in removeQueue) {
      store.removeEntity(it)
    }
  }
}
