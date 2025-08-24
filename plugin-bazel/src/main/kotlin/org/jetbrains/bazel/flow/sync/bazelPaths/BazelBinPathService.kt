package org.jetbrains.bazel.flow.sync.bazelPaths

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@State(
  name = "BazelBinPathService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
class BazelBinPathService : PersistentStateComponent<BazelBinPathService.State> {
  var bazelBinPath: String? = null
  var bazelExecPath: String? = null

  override fun getState(): State = State(bazelBinPath, bazelExecPath)

  override fun loadState(state: State) {
    bazelBinPath = state.bazelBin
    bazelExecPath = state.execRoot
  }

  companion object {
    fun getInstance(project: Project): BazelBinPathService = project.service<BazelBinPathService>()
  }

  data class State(var bazelBin: String? = null, var execRoot: String? = null)
}
