package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

internal class BazelExperimentalProjectSettingsConfigurable(private val project: Project) : SearchableConfigurable {
  // experimental features
  private val enableLocalJvmActionsCheckBox: JBCheckBox
  private val hotswapEnabledCheckBox: JBCheckBox
  private val useIntellijTestRunnerCheckBox: JBCheckBox
  private val enableBuildWithJpsCheckBox: JBCheckBox
  private val enableQueryTabCheckbox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    // TODO: BAZEL-1837
    // experimental features
    useIntellijTestRunnerCheckBox = initUseIntellijTestRunnerCheckBoxBox()
    enableLocalJvmActionsCheckBox = initEnableLocalJvmActionsCheckBox()
    hotswapEnabledCheckBox = initHotSwapEnabledCheckBox()

    enableBuildWithJpsCheckBox = initEnableBuildWithJpsCheckBox()
    enableQueryTabCheckbox = initEnableQueryTabCheckBox()
  }

  private fun initEnableLocalJvmActionsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.local.jvm.actions.checkbox.text")).apply {
      isSelected = currentProjectSettings.enableLocalJvmActions
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(enableLocalJvmActions = isSelected)
        useIntellijTestRunnerCheckBox.isEnabled = isSelected
        hotswapEnabledCheckBox.isEnabled = isSelected && !BazelFeatureFlags.fastBuildEnabled
      }
    }

  private fun initUseIntellijTestRunnerCheckBoxBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.use.intellij.test.runner.checkbox.text")).apply {
      isSelected = currentProjectSettings.useIntellijTestRunner
      isEnabled = currentProjectSettings.enableLocalJvmActions
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(useIntellijTestRunner = isSelected)
      }
    }

  private fun initHotSwapEnabledCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.hotswap.enabled.checkbox.text")).apply {
      // hotswap now only works with local JVM actions
      isEnabled = currentProjectSettings.enableLocalJvmActions && !BazelFeatureFlags.fastBuildEnabled
      isSelected = currentProjectSettings.hotSwapEnabled
      addItemListener {
        if (currentProjectSettings.enableLocalJvmActions) {
          currentProjectSettings = currentProjectSettings.withNewHotSwapEnabled(isSelected)
        }
      }
    }

  private fun initEnableBuildWithJpsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.build.with.jps.checkbox.text")).apply {
      isSelected = currentProjectSettings.enableBuildWithJps
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(enableBuildWithJps = isSelected)
      }
    }

  private fun initEnableQueryTabCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.query.tab.checkbox.text")).apply {
      isSelected = currentProjectSettings.enableQueryTab
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(enableQueryTab = isSelected)
      }
    }

  override fun createComponent(): JComponent {
    val resetToolPanelButton =
      JButton("Restart window panel").apply {
        addActionListener {
          BazelSettingsPanelEventSubscriber.runActions(
            BazelSettingsPanelEventSubscriber.BazelSettingsPanelEventType.RESET_TOOL_WINDOW_BUTTON_PRESSED,
          )
        }
      }
    val queryTabCheckboxComponent =
      JPanel().apply {
        layout = FlowLayout()
        add(enableQueryTabCheckbox)
        add(resetToolPanelButton)
      }

    return panel {
      row { cell(enableBuildWithJpsCheckBox).align(Align.FILL) }
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
      row {
        cell(queryTabCheckboxComponent).align(Align.FILL)
        contextHelp(BazelPluginBundle.message("project.settings.plugin.enable.query.tab.help.text"))
      }
    }
  }

  override fun isModified(): Boolean = currentProjectSettings != project.bazelProjectSettings

  override fun apply() {
    val isEnableBuildWithJpsChanged = currentProjectSettings.enableBuildWithJps != project.bazelProjectSettings.enableBuildWithJps

    project.bazelProjectSettings = currentProjectSettings

    if (isEnableBuildWithJpsChanged) {
      BazelCoroutineService.getInstance(project).start {
        ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
      }
    }
  }

  override fun reset() {
    super.reset()
    currentProjectSettings = project.bazelProjectSettings
  }

  override fun getDisplayName(): String = BazelPluginBundle.message(DISPLAY_NAME_KEY)

  override fun getId(): String = ID

  companion object {
    const val ID = "bazel.experimental.project.settings"
    const val DISPLAY_NAME_KEY = "project.settings.experimental.settings"
  }

  object SearchIndex { // the companion object of a Configurable is not allowed to have non-const members
    val keys =
      listOf(
        "project.settings.plugin.enable.local.jvm.actions.checkbox.text",
        "project.settings.plugin.hotswap.enabled.checkbox.text",
        "project.settings.plugin.use.intellij.test.runner.checkbox.text",
      )
  }
}

internal class BazelExperimentalProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
  override fun createConfigurable(): Configurable = BazelExperimentalProjectSettingsConfigurable(project)
}
