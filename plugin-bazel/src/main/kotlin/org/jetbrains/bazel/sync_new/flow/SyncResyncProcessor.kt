package org.jetbrains.bazel.sync_new.flow

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.languages.projectview.ProjectViewWorkspaceContextProvider
import org.jetbrains.bazel.sync_new.SyncFlagsService
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.InfoProperty
import org.jetbrains.bazel.sync_new.connector.release
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.workspace
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.putNullable
import org.jetbrains.bazel.sync_new.storage.hash.putUnordered
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import kotlin.io.path.absolutePathString

internal class SyncResyncProcessor(private val project: Project) {
  suspend fun computeResyncState(task: SyncConsoleTask): SyncResyncState {
    val store = project.service<SyncStoreService>()
    val resync = computeWorkspaceConfig(task, store)
                 || computeSyncConfig(store)
                 || computeBazelVersion(task, store)
    return if (resync) {
      SyncResyncState.FORCE_RESYNC
    }
    else {
      SyncResyncState.INCREMENTAL
    }
  }

  private fun computeWorkspaceConfig(task: SyncConsoleTask, store: SyncStoreService): Boolean {
    val newWorkspaceHash = hashWorkspaceConfig(project)
    val changed = newWorkspaceHash != store.syncMetadata.get().workspaceConfigHash
    store.syncMetadata.modify { it.copy(workspaceConfigHash = newWorkspaceHash) }
    return changed
  }

  private fun computeSyncConfig(store: SyncStoreService): Boolean {
    val newSyncHash = hashSyncConfig(project)
    val changed = newSyncHash != store.syncMetadata.get().syncConfigHash
    store.syncMetadata.modify { it.copy(syncConfigHash = newSyncHash) }
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

  fun hashWorkspaceConfig(project: Project): HashValue128 = hash {
    val workspaceContext = ProjectViewWorkspaceContextProvider.getInstance(project)
      .readWorkspaceContext()

    putByte(1) // mark
    putNullable(workspaceContext.bazelBinary) { putString(it.absolutePathString()) }
    putUnordered(workspaceContext.buildFlags) { putString(it) }
    putUnordered(workspaceContext.syncFlags) { putString(it) }
    putUnordered(workspaceContext.enabledRules) { putString(it) }
  }

  fun hashSyncConfig(project: Project): HashValue128 = hash {
    val flags = project.service<SyncFlagsService>()

    // mark
    putByte(1)

    putBoolean(flags.useOptimizedInverseSourceQuery)
    putBoolean(flags.useSkyQueryForInverseSourceQueries)
    putBoolean(flags.useFastSource2Label)
    putBoolean(flags.useFileChangeBasedInvalidation)
    putBoolean(flags.disallowLegacyFullTargetGraphMaterialization)
    putBoolean(flags.useTargetHasher)
  }
}
