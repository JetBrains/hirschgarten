package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.options.BoundCompositeSearchableConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings

internal class BazelExperimentalProjectSettingsConfigurable(private val project: Project) :
  BoundCompositeSearchableConfigurable<UnnamedConfigurable>(
    displayName = BazelPluginBundle.message(DISPLAY_NAME_KEY),
    helpTopic = "",
  ),
  Configurable.WithEpDependencies {
  private var currentProjectSettings = project.bazelProjectSettings

  override fun createPanel(): DialogPanel =
    panel {
      // add settings from extensions
      configurables
        .sortedBy { c -> (c as? Configurable)?.displayName ?: "" }
        .forEach { appendDslConfigurable(it) }
    }

  override fun getDependencies(): List<BaseExtensionPointName<*>> = listOf(BazelExperimentalSettingsProvider.ep)

  override fun createConfigurables(): List<UnnamedConfigurable> =
    project.bazelExperimentalSettingsProviders.map {
      it.createConfigurable(project)
    }

  override fun reset() {
    super<BoundCompositeSearchableConfigurable>.reset()
    currentProjectSettings = project.bazelProjectSettings
  }

  override fun getDisplayName(): String = BazelPluginBundle.message(DISPLAY_NAME_KEY)

  override fun getId(): String = ID

  companion object {
    const val ID = "bazel.experimental.project.settings"
    const val DISPLAY_NAME_KEY = "project.settings.experimental.settings"
  }
}

internal class BazelExperimentalProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelExperimentalProjectSettingsConfigurable(project)
}
