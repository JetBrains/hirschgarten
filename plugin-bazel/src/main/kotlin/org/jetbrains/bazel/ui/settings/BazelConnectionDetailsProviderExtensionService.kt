package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager

data class BazelConnectionDetailsProviderExtensionState(var projectPath: String? = null, var connectionFile: String? = null)

@State(
  name = "BazelConnectionDetailsProviderExtensionService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
class BazelConnectionDetailsProviderExtensionService : PersistentStateComponent<BazelConnectionDetailsProviderExtensionState> {
  var projectPath: VirtualFile? = null
  var connectionFile: VirtualFile? = null

  override fun getState(): BazelConnectionDetailsProviderExtensionState? =
    BazelConnectionDetailsProviderExtensionState(
      projectPath = projectPath?.url,
      connectionFile = connectionFile?.url,
    )

  override fun loadState(state: BazelConnectionDetailsProviderExtensionState) {
    val virtualFileManager = VirtualFileManager.getInstance()

    projectPath = state.projectPath?.let { virtualFileManager.findFileByUrl(it) }
    connectionFile = state.connectionFile?.let { virtualFileManager.findFileByUrl(it) }
  }
}

val Project.stateService: BazelConnectionDetailsProviderExtensionService
  get() = getService(BazelConnectionDetailsProviderExtensionService::class.java)
