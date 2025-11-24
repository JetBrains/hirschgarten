package org.jetbrains.bazel.sync_new.index

interface SyncIndex {
  val name: String
  fun invalidateAll();
}
