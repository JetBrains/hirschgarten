package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.intellij.openapi.diagnostic.Logger
import org.h2.mvstore.MVStore
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists

internal fun createOrResetMvStore(log: Logger, file: Path): MVStore {
  file.createParentDirectories()
  try {
    return tryOpenMvStore(log, file)
  }
  catch (e: Throwable) {
    log.warn("Error loading snapshot storage", e)
  }
  file.deleteIfExists()
  return tryOpenMvStore(log, file)
}

private fun tryOpenMvStore(log: Logger, file: Path): MVStore {
  val store = MVStore.Builder()
    .fileName(file.toAbsolutePath().toString())
    .backgroundExceptionHandler { _, e -> log.warn("Background error in snapshot storage", e) }
    .autoCommitDisabled()
    .cacheSize(16)
    .open()
  store.versionsToKeep = 0
  return store
}
