package org.jetbrains.bazel.sync_new.settings

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundCompositeSearchableConfigurable
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import org.jetbrains.annotations.NonNls
import org.jetbrains.bazel.sync_new.SyncFlagsService
import javax.swing.JCheckBox

data class BazelSyncSettings(
  var enableIncrementalSync: Boolean = false,
  var useTargetHasher: Boolean = true,
) {
  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelSyncSettings = project.service<BazelSyncSettingsService>().settings
  }
}

@Service(Service.Level.PROJECT)
@State(
  name = "BazelSyncSettingsService",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
  reportStatistic = true,
)
internal class BazelSyncSettingsService : DumbAware, PersistentStateComponent<BazelSyncSettings> {
  var settings: BazelSyncSettings = BazelSyncSettings()

  override fun getState(): BazelSyncSettings = settings

  override fun loadState(state: BazelSyncSettings) {
    settings = state
  }
}

// TODO: localize strings
internal class BazelSyncSettingsConfigurable(
  private val project: Project,
) : BoundConfigurable("Incremental sync"), SearchableConfigurable, Configurable.Beta {
  val settings = project.service<BazelSyncSettingsService>().settings

  override fun createPanel(): DialogPanel = panel {
    group("Incremental Sync") {
      lateinit var incrementalSyncEnabled: Cell<JCheckBox>

      row {
        incrementalSyncEnabled = checkBox("Enable incremental sync")
          .bindSelected(settings::enableIncrementalSync)
        icon(AllIcons.General.Beta)
      }

      indent {
        row {
          checkBox("Use target hasher")
            .bindSelected(settings::useTargetHasher)
            .contextHelp(
              description = "Use target hashing for narrowing target changes. " +
                            "Disabling it might reduce time spend on querying bazel.",
            )
        }
      }.visibleIf(incrementalSyncEnabled.selected)
    }
  }

  override fun getId(): @NonNls String = "bazel.sync.settings"

}

internal class BazelSyncProjectSettingsConfigurable(private val project: Project) :
  BoundCompositeSearchableConfigurable<UnnamedConfigurable>(
    displayName = "Sync",
    helpTopic = "",
  ), Configurable.Beta {

  override fun createConfigurables(): List<UnnamedConfigurable> = listOf(
    BazelSyncSettingsConfigurable(
      project = project,
    ),
  )

  override fun createPanel(): DialogPanel = panel {
    configurables.forEach {
      appendDslConfigurable(it)
    }
  }
}

internal class BazelSyncProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelSyncProjectSettingsConfigurable(project)

  override fun canCreateConfigurable(): Boolean = SyncFlagsService.isFeatureEnabled
}
