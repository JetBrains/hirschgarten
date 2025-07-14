package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.workspaceName
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask

class PathSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) =
    coroutineScope {
      environment.withSubtask("Collect bazel workspace info") {
        val bazelBinPathService = BazelBinPathService.getInstance(environment.project)
        val bazelBinPathResult =
          query("workspace/bazelPaths") { environment.server.workspaceBazelPaths() }
        bazelBinPathService.bazelBinPath = bazelBinPathResult.bazelBin
        bazelBinPathService.bazelExecPath = bazelBinPathResult.executionRoot
        val bazelWorkspaceResult =
          query("workspace/bazelWorkspaceName") { environment.server.workspaceName() }

        environment.project.workspaceName = bazelWorkspaceResult.workspaceName
      }
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
  var bazelExecPath: String? = null

  override fun getState(): State? = State(bazelBinPath, bazelExecPath)

  override fun loadState(state: State) {
    bazelBinPath = state.bazelBin
    bazelExecPath = state.execRoot
  }

  companion object {
    fun getInstance(project: Project): BazelBinPathService = project.service<BazelBinPathService>()
  }

  data class State(var bazelBin: String? = null, var execRoot: String? = null)
}
