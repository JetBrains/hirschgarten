package org.jetbrains.bazel.sync_new.flow.vfs_diff

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.flow.PartialTarget
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.SyncConsoleTask
import org.jetbrains.bazel.sync_new.flow.SyncSpec
import org.jetbrains.bazel.sync_new.flow.universe.syncRepoMapping
import org.jetbrains.bazel.sync_new.flow.vfs_diff.processor.SyncVFSChangeProcessor
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.storageContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

// TODO: separate target discovery
@Service(Service.Level.PROJECT)
class SyncVFSService(
  private val project: Project,
) : Disposable {

  private val disposable = Disposer.newDisposable()

  internal val vfsState: FlatStorage<SyncVFSState> = project.storageContext.createFlatStore<SyncVFSState>("bazel.sync.diff.vfsState")
    .withCreator {
      SyncVFSState(
        listenState = SyncVFSListenState.WAITING_FOR_FIRST_SYNC,
      )
    }
    .withCodec { ofKryo() }
    .build()

  internal val vfsListener: SyncVFSListener = SyncVFSListener(project, disposable)

  suspend fun resetAll() {
    project.service<SyncVFSStoreService>().invalidateAll()
    vfsState.modify { state -> state.copy(listenState = SyncVFSListenState.WAITING_FOR_FIRST_SYNC) }
    vfsListener.ensureDetached()
    vfsListener.file2State.clear()
  }

  suspend fun computeVFSDiff(spec: SyncSpec, scope: SyncScope, task: SyncConsoleTask, universeDiff: SyncColdDiff): SyncColdDiff {
    val isFirstSync = vfsState.get().listenState == SyncVFSListenState.WAITING_FOR_FIRST_SYNC
    val ctx = SyncVFSContext(
      project = project,
      storage = project.service<SyncVFSStoreService>(),
      repoMapping = project.syncRepoMapping,
      pathsResolver = LegacyBazelFrontendBridge.fetchBazelPathsResolver(project),
      scope = scope,
      spec = spec,
      isFirstSync = isFirstSync,
      universeDiff = universeDiff,
      flags = project.service(),
      task = task,
    )
    if (isFirstSync) {
      vfsState.modify { state -> state.copy(listenState = SyncVFSListenState.LISTENING_VFS) }
      vfsListener.ensureAttached()
    }
    val watcherChanges = if (spec.skipImplicitFileChanges) {
      mapOf()
    } else {
      consumeWatcherFileChanges()
    }
    val changedFiles = when (scope) {
      is SyncScope.Partial -> {
        scope.targets.filterIsInstance<PartialTarget.ByFile>()
          .map { SyncFileClassifier.classify(it.file) }
      }

      else -> emptyList()
    }
    val watcherDiff = SyncFileDiff(
      removed = watcherChanges[SyncFileState.REMOVED] ?: emptyList(),
      changed = (watcherChanges[SyncFileState.CHANGED] ?: emptyList()) + changedFiles,
      added = watcherChanges[SyncFileState.ADDED] ?: emptyList(),
    )
    return SyncVFSChangeProcessor().processBulk(ctx, watcherDiff)
  }

  private fun consumeWatcherFileChanges(): Map<SyncFileState, List<SyncVFSFile>> {
    val changes = vfsListener.file2State.entries()
      .map { (k, v) -> v to SyncFileClassifier.classify(k.resolveSymlinks()) }
      .groupBy({ (k, _) -> k }, { (_, v) -> v })
      .toMap()
    vfsListener.file2State.clear()
    return changes
  }

  private fun Path.resolveSymlinks(): Path {
    if (!Files.exists(this)) {
      return this
    }
    return try {
      this.toRealPath()
    }
    catch (_: IOException) {
      this
    }
  }

  override fun dispose() {
    Disposer.dispose(disposable)
  }

}
