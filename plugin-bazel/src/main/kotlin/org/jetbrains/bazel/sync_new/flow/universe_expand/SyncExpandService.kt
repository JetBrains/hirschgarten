package org.jetbrains.bazel.sync_new.flow.universe_expand

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.flow.diff.SyncColdDiff
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class SyncExpandService(
  private val project: Project,
) {
  internal val graph: FlatStorage<SyncReachabilityGraph> =
    project.storageContext.createFlatStore<SyncReachabilityGraph>("bazel.sync.universe_expand.graph", StorageHints.USE_IN_MEMORY)
      .withCodec { SyncReachabilityGraph.codec }
      .withCreator { SyncReachabilityGraph() }
      .build()

  suspend fun expandDependencyDiff(diff: SyncColdDiff): SyncColdDiff {
    val ctx = SyncExpandContext(project)
    return SyncExpandProcessor().process(ctx, diff)
  }
}
