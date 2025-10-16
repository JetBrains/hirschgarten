package org.jetbrains.bazel.settings.bazel
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

/**
 * BazelJVMProjectSettings holds the JVM-specific part of settings. It can be modified by BazelJVMSettingsProviders,
 * which injects JVM-specific settings into the Bazel setting panel. However, unlike BazelJVMSettingsProviders, this BazelJVMProjectSettings is always available
 * even there is no java-plugin.
 * * (BazelJVMSettingsProviders is the extension of an extension point, and it will not be loaded when java plugin is absent, so that user won't be able to change t
 * the JVM-specific settings when there is no java plugin)
 * */
data class BazelJVMProjectSettings(
  var hotSwapEnabled: Boolean = true,
  var enableLocalJvmActions: Boolean = false,
  var enableBuildWithJps: Boolean = false,
  var useIntellijTestRunner: Boolean = false,
  var enableKotlinCoroutineDebug: Boolean = false,
) {
  fun withNewHotSwapEnabled(newHotSwapEnabled: Boolean): BazelJVMProjectSettings = copy(hotSwapEnabled = newHotSwapEnabled)
}

@State(
  name = "BazelJVMProjectSettingsService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelJVMProjectSettingsService :
  DumbAware,
  PersistentStateComponent<BazelJVMProjectSettings> {
  var settings: BazelJVMProjectSettings = BazelJVMProjectSettings()

  override fun getState(): BazelJVMProjectSettings = settings

  override fun loadState(settingsState: BazelJVMProjectSettings) {
    this.settings =
      BazelJVMProjectSettings(
        hotSwapEnabled = settingsState.hotSwapEnabled,
        enableLocalJvmActions = settingsState.enableLocalJvmActions,
        enableBuildWithJps = settingsState.enableBuildWithJps,
        useIntellijTestRunner = settingsState.useIntellijTestRunner,
        enableKotlinCoroutineDebug = settingsState.enableKotlinCoroutineDebug,
      )
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelJVMProjectSettingsService = project.getService(BazelJVMProjectSettingsService::class.java)
  }
}

var Project.bazelJVMProjectSettings: BazelJVMProjectSettings
  get() = BazelJVMProjectSettingsService.getInstance(this).settings.copy()
  set(value) {
    BazelJVMProjectSettingsService.getInstance(this).settings = value.copy()
  }
