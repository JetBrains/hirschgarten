package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetGraph
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class SyncStoreService(
  private val project: Project,
) {
  val syncMetadata: FlatStorage<SyncMetadata> =
    project.storageContext.createFlatStore<SyncMetadata>("bazel.sync.syncMetadata", StorageHints.USE_IN_MEMORY)
      .withCreator { SyncMetadata() }
      .withCodec { ofKryo() }
      .build()

  val targetGraph: BazelTargetGraph = BazelTargetGraph(project.storageContext)
}
