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
  val selectedServerJdkName: String? = null,
  val customJvmOptions: List<String> = emptyList(),
  val hotSwapEnabled: Boolean = true,
  val showExcludedDirectoriesAsSeparateNode: Boolean = true,
) {
  fun withNewProjectViewPath(newProjectViewFilePath: Path): BazelProjectSettings = copy(projectViewPath = newProjectViewFilePath)

  fun withNewServerJdkName(newServerJdkName: String?): BazelProjectSettings? =
    newServerJdkName?.let {
      copy(selectedServerJdkName = newServerJdkName)
    }

  fun withNewCustomJvmOptions(newCustomJvmOptions: List<String>): BazelProjectSettings = copy(customJvmOptions = newCustomJvmOptions)

  fun withNewHotSwapEnabled(newHotSwapEnabled: Boolean): BazelProjectSettings = copy(hotSwapEnabled = newHotSwapEnabled)
}

internal data class BazelProjectSettingsState(
  var projectViewPathUri: String? = null,
  var selectedServerJdkName: String? = null,
  var customJvmOptions: List<String> = emptyList(),
  var hotSwapEnabled: Boolean = true,
  var showExcludedDirectoriesAsSeparateNode: Boolean = true,
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
      selectedServerJdkName = settings.selectedServerJdkName,
      customJvmOptions = settings.customJvmOptions,
      hotSwapEnabled = settings.hotSwapEnabled,
      showExcludedDirectoriesAsSeparateNode = settings.showExcludedDirectoriesAsSeparateNode,
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
          selectedServerJdkName = settingsState.selectedServerJdkName,
          customJvmOptions = settingsState.customJvmOptions,
          hotSwapEnabled = settingsState.hotSwapEnabled,
          showExcludedDirectoriesAsSeparateNode = settingsState.showExcludedDirectoriesAsSeparateNode,
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
