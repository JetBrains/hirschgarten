package org.jetbrains.bazel.settings.bazel
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus

/**
 * BazelJVMProjectSettings holds the JVM-specific part of settings. It can be modified by BazelJVMSettingsProviders,
 * which injects JVM-specific settings into the Bazel setting panel. However, unlike BazelJVMSettingsProviders, this BazelJVMProjectSettings is always available
 * even there is no java-plugin.
 * * (BazelJVMSettingsProviders is the extension of an extension point, and it will not be loaded when java plugin is absent, so that user won't be able to change t
 * the JVM-specific settings when there is no java plugin)
 * */
@ApiStatus.Internal
data class BazelJVMProjectSettings(
  val hotSwapEnabled: Boolean = true,
  val enableLocalJvmActions: Boolean = false,
  val enableBuildWithJps: Boolean = false,
  val useIntellijTestRunner: Boolean = false,
  val enableKotlinCoroutineDebug: Boolean = true,
)

@State(
  name = "BazelJVMProjectSettingsService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
@Service(Service.Level.PROJECT)
internal class BazelJVMProjectSettingsService :
  DumbAware,
  PersistentStateComponent<BazelJVMProjectSettingsState> {
  var settings: BazelJVMProjectSettingsState = BazelJVMProjectSettingsState()

  override fun getState(): BazelJVMProjectSettingsState = settings

  override fun loadState(settingsState: BazelJVMProjectSettingsState) {
    this.settings = settingsState
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelJVMProjectSettingsService = project.getService(BazelJVMProjectSettingsService::class.java)
  }
}

/**
 * State for XML serialization
 */
internal data class BazelJVMProjectSettingsState(
  var hotSwapEnabled: Boolean = true,
  var enableLocalJvmActions: Boolean = false,
  var enableBuildWithJps: Boolean = false,
  var useIntellijTestRunner: Boolean = false,
  var enableKotlinCoroutineDebugV2: Boolean = true,  // Default was changed from false to true, "v2" to avoid loading the old key
)

var Project.bazelJVMProjectSettings: BazelJVMProjectSettings
  @ApiStatus.Internal
  get() = with(BazelJVMProjectSettingsService.getInstance(this).settings) {
    BazelJVMProjectSettings(
      hotSwapEnabled = hotSwapEnabled,
      enableLocalJvmActions = enableLocalJvmActions,
      enableBuildWithJps = enableBuildWithJps,
      useIntellijTestRunner = useIntellijTestRunner,
      enableKotlinCoroutineDebug = enableKotlinCoroutineDebugV2,
    )
  }
  @ApiStatus.Internal
  set(value) {
    BazelJVMProjectSettingsService.getInstance(this).settings = with(value) {
      BazelJVMProjectSettingsState(
        hotSwapEnabled = hotSwapEnabled,
        enableLocalJvmActions = enableLocalJvmActions,
        enableBuildWithJps = enableBuildWithJps,
        useIntellijTestRunner = useIntellijTestRunner,
        enableKotlinCoroutineDebugV2 = enableKotlinCoroutineDebug,
      )
    }
  }
