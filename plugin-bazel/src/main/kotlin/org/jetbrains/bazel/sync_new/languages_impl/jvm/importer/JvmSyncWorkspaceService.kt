package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.lang.store.persistent.createPersistentIncrementalEntityStore
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class JvmSyncWorkspaceService(
  private val project: Project,
) {
  internal val storage = createPersistentIncrementalEntityStore<JvmResourceId, JvmModuleEntity>(
    storageContext = project.storageContext,
    name = "jvm_sync_storage",
    resourceIdCodec = { ofKryo() },
    entityCodec = { ofKryo() },
    idHasher = { hash() },
  )
}
