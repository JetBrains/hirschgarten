package org.jetbrains.bazel.utils.store.mvstore

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.util.io.mvstore.createOrResetMvStore
import org.h2.mvstore.MVStore

object MVStoreUtils {
  fun openStoreForProject(project: Project, name: String): MVStore {
    val file = project.getProjectDataPath("kv-store-$name.db")
    return createOrResetMvStore(file = file, readOnly = false, logSupplier = { logger<MVStoreKVStore<*, *>>() })
  }
}
