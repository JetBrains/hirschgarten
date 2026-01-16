package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.InfoProperty
import org.jetbrains.bazel.sync_new.connector.release
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.workspace
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal class SyncResyncProcessor(private val project: Project) {
  suspend fun computeResyncState(task: SyncConsoleTask): SyncResyncState {
    val store = project.service<SyncStoreService>()

    val resync = computeWorkspaceConfig(task, store)
                 || computeBazelVersion(task, store)
    return if (resync) {
      SyncResyncState.FORCE_RESYNC
    }
    else {
      SyncResyncState.INCREMENTAL
    }
  }

  private fun computeWorkspaceConfig(task: SyncConsoleTask, store: SyncStoreService): Boolean {
    val newWorkspaceHash = SyncConfig.hashWorkspaceConfig(project)
    val changed = newWorkspaceHash != store.syncMetadata.get().configHash
    store.syncMetadata.modify { it.copy(configHash = newWorkspaceHash) }
    return changed
  }

  private suspend fun computeBazelVersion(task: SyncConsoleTask, store: SyncStoreService): Boolean {
    val connector = project.service<BazelConnectorService>().ofSyncTask(task)
    val result = connector.info {
      release()
      workspace()
    }
    val release = result.unwrap().properties.firstIsInstanceOrNull<InfoProperty.Release>() ?: return false
    val newVersion = release.version
    val changed = newVersion != store.syncMetadata.get().bazelVersion
    store.syncMetadata.modify { it.copy(bazelVersion = newVersion) }
    return changed
  }
}
