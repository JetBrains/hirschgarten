package org.jetbrains.bazel.sync_new.storage

/**
 * Lossy hints for basic customization of the required store
 */
interface StorageHints {

}

enum class DefaultStorageHints : StorageHints {

  /**
   * Hint creation of storage that supports some kind of incremental loading/saving
   */
  USE_PAGED_STORE,

  /**
   * Hint creation of full in-memory persistent storage.
   *
   * Usually implemented as saving blob on register and saving it on disposal
   */
  USE_IN_MEMORY
}
