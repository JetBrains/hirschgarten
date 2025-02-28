package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.task.query

class BazelBinPathSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) =
    coroutineScope {
      val bazelBinPathService = BazelBinPathService.getInstance(environment.project)
      val bazelBinPathResult =
        query("workspace/bazelBinPath") { environment.server.workspaceBazelBinPath() }
      bazelBinPathService.bazelBinPath = bazelBinPathResult.path
    }
}

@State(
  name = "BazelBinPathService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
class BazelBinPathService : PersistentStateComponent<BazelBinPathService.State> {
  var bazelBinPath: String? = null

  override fun getState(): State? = bazelBinPath?.let { State(it) }

  override fun loadState(state: State) {
    bazelBinPath = state.path
  }

  companion object {
    fun getInstance(project: Project): BazelBinPathService = project.service<BazelBinPathService>()
  }

  data class State(var path: String? = null)
}
