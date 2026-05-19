package org.jetbrains.bazel.project

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class BazelProjectPropertiesState(
  var workspaceName: String? = null,
)

@Service(Service.Level.PROJECT)
@State(
  name = "BazelProjectProperties",
  storages = [Storage(StoragePathMacros.CACHE_FILE)],
)
@ApiStatus.Internal
class BazelProjectProperties : PersistentStateComponent<BazelProjectPropertiesState> {
  var workspaceName: String? = null

  override fun getState(): BazelProjectPropertiesState =
    BazelProjectPropertiesState(
      workspaceName = workspaceName
    )

  override fun loadState(state: BazelProjectPropertiesState) {
    workspaceName = state.workspaceName
  }

  companion object {
    val Project.bazelProjectProperties: BazelProjectProperties
      @ApiStatus.Internal
      get() = service<BazelProjectProperties>()
  }
}
