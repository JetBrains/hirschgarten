package org.jetbrains.bazel.sync.workspace.persistence

import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.autolink.mapExtensionSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot

@ApiStatus.Internal
class WorkspaceSnapshotServiceImpl(
  private val project: Project,
  private val coroutineScope: CoroutineScope,
) : WorkspaceSnapshotService, SettingsSavingComponent, Disposable {
  private val delegate: WorkspaceSnapshotService by lazy {
    when {
      (isEphemeral || !project.isBazelProject) && !forcePersistent -> InMemoryWorkspaceSnapshotService()
      else -> PersistentWorkspaceSnapshotService(project, coroutineScope)
    }
  }

  var isEphemeral: Boolean = false

  @TestOnly
  var forcePersistent: Boolean = false

  override val snapshot: StateFlow<WorkspaceSnapshot>
    get() = delegate.snapshot

  override suspend fun currentSnapshot(): WorkspaceSnapshot = delegate.currentSnapshot()
  override suspend fun <R> update(updater: WorkspaceSnapshotUpdater<R>): Pair<WorkspaceSnapshot, R> = delegate.update(updater)

  override suspend fun save() {
    (delegate as? SettingsSavingComponent)?.save()
  }

  override fun dispose() {
    (delegate as? Disposable)?.apply { Disposer.dispose(this) }
  }
}

internal class InMemoryWorkspaceSnapshotService : WorkspaceSnapshotService {
  private val updateLock = Mutex()

  override val snapshot: StateFlow<WorkspaceSnapshot>
    field = MutableStateFlow(WorkspaceSnapshot.EMPTY)

  override suspend fun currentSnapshot(): WorkspaceSnapshot = snapshot.value

  override suspend fun <R> update(updater: WorkspaceSnapshotUpdater<R>): Pair<WorkspaceSnapshot, R> {
    // not using atomic `update { ... }` to ensure single invocation of updater
    return updateLock.withLock {
      val current = snapshot.value
      val (newSnapshot, result) = updater.update(current)
      snapshot.value = newSnapshot
      newSnapshot to result
    }
  }

}
