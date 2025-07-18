package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.projectAware.BazelProjectAware
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import javax.swing.JComponent

class BazelJVMExperimentalSettingsProvider : BazelSettingsProvider {
  override fun createConfigurable(project: Project): UnnamedConfigurable = BazelJVMExperimentalConfigurable(project)

  override fun searchIndexKeys() =
    listOf(
      "project.settings.plugin.enable.local.jvm.actions.checkbox.text",
      "project.settings.plugin.hotswap.enabled.checkbox.text",
      "project.settings.plugin.use.intellij.test.runner.checkbox.text",
    )
}

class BazelJVMExperimentalConfigurable(private val project: Project) : UnnamedConfigurable {
  private val enableLocalJvmActionsCheckBox: JBCheckBox
  private val enableBuildWithJpsCheckBox: JBCheckBox
  private val useIntellijTestRunnerCheckBox: JBCheckBox
  private val hotswapEnabledCheckBox: JBCheckBox
  private val enableKotlinCoroutineDebugCheckBox: JBCheckBox

  private var currentJVMProjectSettings = project.bazelJVMProjectSettings

  init {

    enableLocalJvmActionsCheckBox = initEnableLocalJvmActionsCheckBox()
    enableBuildWithJpsCheckBox = initEnableBuildWithJpsCheckBox()
    // TODO: BAZEL-1837
    useIntellijTestRunnerCheckBox = initUseIntellijTestRunnerCheckBoxBox()
    hotswapEnabledCheckBox = initHotSwapEnabledCheckBox()
    enableKotlinCoroutineDebugCheckBox = initEnableKotlinCoroutineDebugCheckBox()
  }

  override fun createComponent(): JComponent =
    panel {
      row { cell(enableKotlinCoroutineDebugCheckBox).align(Align.FILL) }
      group(BazelPluginBundle.message("project.settings.local.runner.settings")) {
        row { cell(enableLocalJvmActionsCheckBox).align(Align.FILL) }
        row {
          cell(hotswapEnabledCheckBox).align(Align.FILL)
          contextHelp(BazelPluginBundle.message("project.settings.plugin.hotswap.enabled.checkbox.help.text"))
        }
        row {
          cell(useIntellijTestRunnerCheckBox).align(Align.FILL)
          contextHelp(BazelPluginBundle.message("project.settings.plugin.use.intellij.test.runner.help.text"))
        }
      }

      row { cell(enableBuildWithJpsCheckBox).align(Align.FILL) }
    }

  override fun isModified(): Boolean = currentJVMProjectSettings != project.bazelJVMProjectSettings

  override fun apply() {
    val isEnableBuildWithJpsChanged = currentJVMProjectSettings.enableBuildWithJps != project.bazelJVMProjectSettings.enableBuildWithJps
    if (isEnableBuildWithJpsChanged) {
      BazelProjectAware.notify(project)
    }
    project.bazelJVMProjectSettings = currentJVMProjectSettings
  }

  private fun initEnableLocalJvmActionsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.local.jvm.actions.checkbox.text")).apply {
      isSelected = currentJVMProjectSettings.enableLocalJvmActions
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.copy(enableLocalJvmActions = isSelected)
        useIntellijTestRunnerCheckBox.isEnabled = isSelected
        hotswapEnabledCheckBox.isEnabled = isSelected && !BazelFeatureFlags.fastBuildEnabled
        enableKotlinCoroutineDebugCheckBox.isEnabled = !isSelected
      }
    }

  private fun initUseIntellijTestRunnerCheckBoxBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.use.intellij.test.runner.checkbox.text")).apply {
      isSelected = currentJVMProjectSettings.useIntellijTestRunner
      isEnabled = currentJVMProjectSettings.enableLocalJvmActions
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.copy(useIntellijTestRunner = isSelected)
      }
    }

  private fun initHotSwapEnabledCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.hotswap.enabled.checkbox.text")).apply {
      // hotswap now only works with local JVM actions
      isEnabled = currentJVMProjectSettings.enableLocalJvmActions && !BazelFeatureFlags.fastBuildEnabled
      isSelected = currentJVMProjectSettings.hotSwapEnabled
      addItemListener {
        if (currentJVMProjectSettings.enableLocalJvmActions) {
          currentJVMProjectSettings = currentJVMProjectSettings.withNewHotSwapEnabled(isSelected)
        }
      }
    }

  private fun initEnableBuildWithJpsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.build.with.jps.checkbox.text")).apply {
      isSelected = currentJVMProjectSettings.enableBuildWithJps
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.copy(enableBuildWithJps = isSelected)
        enableKotlinCoroutineDebugCheckBox.isEnabled = !isSelected
      }
    }

  private fun initEnableKotlinCoroutineDebugCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.kotlin.coroutine.debug.checkbox.text")).apply {
      isSelected = currentJVMProjectSettings.enableKotlinCoroutineDebug
      // this setting does not work with the JPS build or local JVM actions
      isEnabled = !currentJVMProjectSettings.enableBuildWithJps && !currentJVMProjectSettings.enableLocalJvmActions
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.copy(enableKotlinCoroutineDebug = isSelected)
      }
    }
}
