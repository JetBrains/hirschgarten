package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import javax.swing.JComponent

internal class BazelExperimentalProjectSettingsConfigurable(private val project: Project) : SearchableConfigurable {
  // experimental features
  private val enableLocalJvmActionsCheckBox: JBCheckBox
  private val useIntellijTestRunnerCheckBox: JBCheckBox
  private val enableBuildWithJpsCheckBox: JBCheckBox

  private var currentProjectSettings = project.bazelProjectSettings

  init {
    // TODO: BAZEL-1837
    // experimental features
    useIntellijTestRunnerCheckBox = initUseIntellijTestRunnerCheckBoxBox()
    enableLocalJvmActionsCheckBox = initEnableLocalJvmActionsCheckBox()

    enableBuildWithJpsCheckBox = initEnableBuildWithJpsCheckBox()
  }

  private fun initEnableLocalJvmActionsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.local.jvm.actions.checkbox.text")).apply {
      isSelected = currentProjectSettings.enableLocalJvmActions
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(enableLocalJvmActions = isSelected)
        useIntellijTestRunnerCheckBox.isEnabled = isSelected
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

  private fun initEnableBuildWithJpsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.build.with.jps.checkbox.text")).apply {
      isSelected = currentProjectSettings.enableBuildWithJps
      addItemListener {
        currentProjectSettings = currentProjectSettings.copy(enableBuildWithJps = isSelected)
      }
    }

  override fun createComponent(): JComponent =
    panel {
      row { cell(enableBuildWithJpsCheckBox).align(Align.FILL) }
      group(BazelPluginBundle.message("project.settings.local.runner.settings")) {
        row { cell(enableLocalJvmActionsCheckBox).align(Align.FILL) }
        row {
          cell(useIntellijTestRunnerCheckBox).align(Align.FILL)
          contextHelp(BazelPluginBundle.message("project.settings.plugin.use.intellij.test.runner.help.text"))
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
