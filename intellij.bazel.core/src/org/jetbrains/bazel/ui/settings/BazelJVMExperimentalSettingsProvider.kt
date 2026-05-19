package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import javax.swing.JComponent

internal class BazelJVMExperimentalSettingsProvider : BazelSettingsProvider {
  override fun createConfigurable(project: Project): UnnamedConfigurable = BazelJVMExperimentalConfigurable(project)

  override fun searchIndexKeys() =
    listOf(
      "project.settings.plugin.hotswap.enabled.checkbox.text",
    )
}

internal class BazelJVMExperimentalConfigurable(private val project: Project) : UnnamedConfigurable {
  private val hotswapEnabledCheckBox: JBCheckBox
  private val enableKotlinCoroutineDebugCheckBox: JBCheckBox

  private var currentJVMProjectSettings = project.bazelJVMProjectSettings

  init {
    hotswapEnabledCheckBox = initHotSwapEnabledCheckBox()
    enableKotlinCoroutineDebugCheckBox = initEnableKotlinCoroutineDebugCheckBox()
  }

  override fun createComponent(): JComponent =
    panel {
      row {
        cell(hotswapEnabledCheckBox)
          .contextHelp(BazelPluginBundle.message("project.settings.plugin.hotswap.enabled.checkbox.help.text"))
      }
      row { cell(enableKotlinCoroutineDebugCheckBox) }
    }

  override fun isModified(): Boolean = currentJVMProjectSettings != project.bazelJVMProjectSettings

  override fun apply() {
    project.bazelJVMProjectSettings = currentJVMProjectSettings
  }

  private fun initHotSwapEnabledCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.hotswap.enabled.checkbox.text")).apply {
      isEnabled = !BazelFeatureFlags.fastBuildEnabled
      isSelected = currentJVMProjectSettings.hotSwapEnabled
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.copy(hotSwapEnabled = isSelected)
      }
    }

  private fun initEnableKotlinCoroutineDebugCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.kotlin.coroutine.debug.checkbox.text")).apply {
      isSelected = currentJVMProjectSettings.enableKotlinCoroutineDebug
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.copy(enableKotlinCoroutineDebug = isSelected)
      }
    }
}
