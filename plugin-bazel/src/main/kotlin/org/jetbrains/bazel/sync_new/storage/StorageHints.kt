package org.jetbrains.bazel.sync_new.storage

interface StorageHints {

}

enum class DefaultStorageHints : StorageHints {
  USE_PAGED_STORE,
  USE_IN_MEMORY
}
