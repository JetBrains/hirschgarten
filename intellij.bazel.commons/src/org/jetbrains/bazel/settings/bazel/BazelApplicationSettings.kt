package org.jetbrains.bazel.settings.bazel

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
@Service(Service.Level.APP)
@State(
  name = "BazelApplicationSettingsService",
  storages = [
    Storage(
      "bazel.application.settings.xml",
      roamingType = RoamingType.DISABLED,
    ),
  ],
)
class BazelApplicationSettingsService : PersistentStateComponent<BazelApplicationSettings> {
  var settings: BazelApplicationSettings = BazelApplicationSettings()

  override fun getState(): BazelApplicationSettings = settings

  override fun loadState(state: BazelApplicationSettings) {
    settings = state
  }

  companion object {
    @JvmStatic
    fun getInstance(): BazelApplicationSettingsService = service()
  }
}

@ApiStatus.Internal
data class BazelApplicationSettings(
  var updateChannel: UpdateChannel = UpdateChannel.RELEASE,
  var enablePhasedSync: Boolean = false,
  var forwardProxySettings: Boolean = false,
)
