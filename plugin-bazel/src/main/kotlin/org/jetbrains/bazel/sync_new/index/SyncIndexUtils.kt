package org.jetbrains.bazel.sync_new.index

internal object SyncIndexUtils {
  internal fun toStorageName(indexName: String) = "bazel.sync.index.$indexName"
}
