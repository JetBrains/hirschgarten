package org.jetbrains.bazel.sync_new.index

import org.jetbrains.bazel.sync_new.storage.StorageContext

interface SyncIndexContext {
  val storageContext: StorageContext

  fun register(index: SyncIndex)
  fun unregister(index: SyncIndex)
}
