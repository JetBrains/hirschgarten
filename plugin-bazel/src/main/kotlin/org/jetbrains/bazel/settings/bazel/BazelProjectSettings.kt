package org.jetbrains.bazel.settings.bazel

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths

data class BazelProjectSettings(
  val projectViewPath: Path? = null,
  val hotSwapEnabled: Boolean = true,
  val showExcludedDirectoriesAsSeparateNode: Boolean = true,
  val enableLocalJvmActions: Boolean = false,
  val enableBuildWithJps: Boolean = false,
) {
  fun withNewProjectViewPath(newProjectViewFilePath: Path): BazelProjectSettings = copy(projectViewPath = newProjectViewFilePath)

  fun withNewHotSwapEnabled(newHotSwapEnabled: Boolean): BazelProjectSettings = copy(hotSwapEnabled = newHotSwapEnabled)
}

internal data class BazelProjectSettingsState(
  var projectViewPathUri: String? = null,
  var hotSwapEnabled: Boolean = true,
  var showExcludedDirectoriesAsSeparateNode: Boolean = true,
  var enableLocalJvmActions: Boolean = false,
  var enableBuildWithJps: Boolean = false,
) {
  fun isEmptyState(): Boolean = this == BazelProjectSettingsState()
}

@State(
  name = "BazelProjectSettingsService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelProjectSettingsService :
  DumbAware,
  PersistentStateComponent<BazelProjectSettingsState> {
  var settings: BazelProjectSettings = BazelProjectSettings()

  override fun getState(): BazelProjectSettingsState =
    BazelProjectSettingsState(
      projectViewPathUri = settings.projectViewPath?.toUri()?.toString(),
      hotSwapEnabled = settings.hotSwapEnabled,
      showExcludedDirectoriesAsSeparateNode = settings.showExcludedDirectoriesAsSeparateNode,
      enableLocalJvmActions = settings.enableLocalJvmActions,
      enableBuildWithJps = settings.enableBuildWithJps,
    )

  override fun loadState(settingsState: BazelProjectSettingsState) {
    if (!settingsState.isEmptyState()) {
      this.settings =
        BazelProjectSettings(
          projectViewPath =
            settingsState.projectViewPathUri?.takeIf { it.isNotBlank() }?.let {
              Paths.get(
                URI(it),
              )
            },
          hotSwapEnabled = settingsState.hotSwapEnabled,
          showExcludedDirectoriesAsSeparateNode = settingsState.showExcludedDirectoriesAsSeparateNode,
          enableLocalJvmActions = settingsState.enableLocalJvmActions,
          enableBuildWithJps = settingsState.enableBuildWithJps,
        )
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelProjectSettingsService = project.getService(BazelProjectSettingsService::class.java)
  }
}

var Project.bazelProjectSettings: BazelProjectSettings
  get() = BazelProjectSettingsService.getInstance(this).settings.copy()
  set(value) {
    BazelProjectSettingsService.getInstance(this).settings = value.copy()
  }
