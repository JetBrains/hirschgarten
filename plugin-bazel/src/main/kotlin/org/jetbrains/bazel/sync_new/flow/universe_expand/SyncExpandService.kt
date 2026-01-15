package org.jetbrains.bazel.sync_new.flow.universe_expand

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.SyncConsoleTask
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.DefaultStorageHints
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class SyncExpandService(
  private val project: Project,
) {
  internal val graph: FlatStorage<SyncReachabilityGraph> =
    project.storageContext.createFlatStore<SyncReachabilityGraph>("bazel.sync.universe_expand.graph", DefaultStorageHints.USE_IN_MEMORY)
      .withCodec { SyncReachabilityGraph.codec }
      .withCreator { SyncReachabilityGraph() }
      .build()

  suspend fun expandDependencyDiff(scope: SyncScope, task: SyncConsoleTask, diff: SyncColdDiff): SyncColdDiff {
    val ctx = SyncExpandContext(
      project = project,
      service = this,
      scope = scope,
    )
    return SyncExpandProcessor().process(ctx, task, diff)
  }

  fun isWithinUniverseScope(target: Label): Boolean {
    val graph = graph.get()
    val id = graph.label2Id.getInt(target)
    if (id == SyncReachabilityGraph.EMPTY_ID) {
      return false
    }
    return graph.universeVertices.contains(id)
  }
}
