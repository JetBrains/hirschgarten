package org.jetbrains.bazel.sync_new.flow.diff.vfs

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.getOrThrow
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.toProtoTargets
import org.jetbrains.bazel.sync_new.flow.diff.query.QueryTargetPattern
import org.jetbrains.bazel.sync_new.flow.diff.vfs.processor.SyncVFSChangeProcessor
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.createFlatStore
import org.jetbrains.bazel.sync_new.storage.storageContext
import kotlin.io.path.extension
import kotlin.io.path.name

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
    vfsListener.ensureDisconnected()
    vfsListener.fileStates.clear()
  }

  suspend fun computeColdDiff(): SyncColdDiff {
    val ctx = SyncVFSContext(
      project = project,
      storage = project.service<SyncVFSStoreService>(),
      repoMapping = LegacyBazelFrontendBridge.fetchRepoMapping(project),
      pathsResolver = LegacyBazelFrontendBridge.fetchBazelPathsResolver(project),
    )
    val isPreFirstSync = vfsState.get().listenState == SyncVFSListenState.WAITING_FOR_FIRST_SYNC
    val changes = if (isPreFirstSync) {
      vfsState.modify { state -> state.copy(listenState = SyncVFSListenState.LISTENING_VFS) }
      computeDiscoveredFileChanges(ctx)
    } else {
      computeWatcherFileChanges()
    }
    val fileDiff = SyncFileDiff(
      removed = changes[SyncFileState.REMOVED] ?: emptyList(),
      changed = changes[SyncFileState.CHANGED] ?: emptyList(),
      added = changes[SyncFileState.ADDED] ?: emptyList(),
    )
    val coldDiff = SyncVFSChangeProcessor().processBulk(ctx, fileDiff)
    return coldDiff
  }

  private suspend fun computeWatcherFileChanges(): Map<SyncFileState, List<SyncVFSFile>> {
    return vfsListener.fileStates.map { (k, v) -> v to SyncFileClassifier.classify(k) }
      .groupBy({ (k, _) -> k }, { (_, v) -> v })
      .toMap()
  }

  private suspend fun computeDiscoveredFileChanges(ctx: SyncVFSContext): Map<SyncFileState, List<SyncVFSFile>> {
    val connector = project.service<BazelConnectorService>().ofLegacyTask()

    val universe = QueryTargetPattern.getProjectTargetUniverse(project)
    val result = connector.query {
      defaults()
      keepGoing()
      output(QueryOutput.PROTO)
      query("buildfiles(${QueryTargetPattern.createUniverseQuery(universe, includeDeps = false)})")
    }
    val internalRepos = LegacyBazelFrontendBridge.fetchWorkspaceContext(project)
      .targets
      .filterIsInstance<ExcludableValue.Included<Label>>()
      .map { it.value.assumeResolved() }
      .map { it.repo.repoName }
      .toSet()
    // TODO: filter only internal repositories
    val legacyRepoMapping = LegacyBazelFrontendBridge.toLegacyRepoMapping(ctx.repoMapping)
    val targets = result.getOrThrow().toProtoTargets()
    val addedFiles = targets.asSequence()
      .filter { it.hasSourceFile() }
      .mapNotNull { it.sourceFile }
      .map { Label.parse(it.name) }
      .filter { internalRepos.contains(it.assumeResolved().repo.repoName) }
      .map { ctx.pathsResolver.toFilePath(it.assumeResolved(), legacyRepoMapping) }
      .mapNotNull { path ->
        val name = path.name
        val ext = path.extension
        when {
          name == "BUILD.bazel" || name == "BUILD" -> SyncVFSFile.BuildFile(path)
          ext == "bzl" -> SyncVFSFile.StarlarkFile(path)
          else -> null
        }
      }
      .toList()
    return mapOf(SyncFileState.ADDED to addedFiles)
  }

  override fun dispose() {
    Disposer.dispose(disposable)
  }

}
