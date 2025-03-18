package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.task.query

class BazelBinPathSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) =
    coroutineScope {
      val bazelBinPathService = BazelBinPathService.getInstance(environment.project)
      val bazelBinPathResult =
        query("workspace/bazelBinPath") { environment.server.workspaceBazelBinPath() }
      bazelBinPathService.state = bazelBinPathService.state.copy(path = bazelBinPathResult.path)
    }
}

@State(
  name = "BazelBinPathService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
class BazelBinPathService : SerializablePersistentStateComponent<BazelBinPathService.State>(State()) {
  companion object {
    fun getInstance(project: Project): BazelBinPathService = project.service<BazelBinPathService>()
  }

  @Serializable
  data class State(val path: String? = null)
}
