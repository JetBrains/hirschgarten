package org.jetbrains.bazel.new_sync.storage

import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner

class NoopPersistentStoreOwner : PersistentStoreOwner {
  override fun register(store: FlatPersistentStore) {

  }

  override fun unregister(store: FlatPersistentStore) {

  }
}
