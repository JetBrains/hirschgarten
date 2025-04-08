package org.jetbrains.bazel.ui.settings

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask

class BazelJVMSettingsProvider(private val project: Project) : BazelSettingsProvider {
  private val hotswapEnabledCheckBox: JBCheckBox

  // experimental features
  private val enableLocalJvmActionsCheckBox: JBCheckBox
  private val enableBuildWithJpsCheckBox: JBCheckBox
  private val useIntellijTestRunnerCheckBox: JBCheckBox

  private var currentJVMProjectSettings = project.bazelJVMProjectSettings

  init {
    hotswapEnabledCheckBox = initHotSwapEnabledCheckBox()

    // experimental features
    enableLocalJvmActionsCheckBox = initEnableLocalJvmActionsCheckBox()
    enableBuildWithJpsCheckBox = initEnableBuildWithJpsCheckBox()
    // TODO: BAZEL-1837
    useIntellijTestRunnerCheckBox = initUseIntellijTestRunnerCheckBoxBox()
  }

  override fun addGeneralSettings(): Panel.() -> Unit =
    {
      row { cell(hotswapEnabledCheckBox).align(Align.FILL) }
    }

  override fun addExperimentalSettings(): Panel.() -> Unit =
    {
      group(BazelPluginBundle.message("project.settings.local.runner.settings")) {
        row { cell(enableLocalJvmActionsCheckBox).align(Align.FILL) }
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
    if (isEnableBuildWithJpsChanged && !project.isSyncInProgress()) {
      BazelCoroutineService.getInstance(project).start {
        ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
      }
    }
    project.bazelJVMProjectSettings = currentJVMProjectSettings
  }

  private fun initEnableLocalJvmActionsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.local.jvm.actions.checkbox.text")).apply {
      isSelected = currentJVMProjectSettings.enableLocalJvmActions
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.copy(enableLocalJvmActions = isSelected)
        // useIntellijTestRunnerCheckBox.isEnabled = isSelected
      }
    }

  private fun initEnableBuildWithJpsCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.enable.build.with.jps.checkbox.text")).apply {
      isSelected = currentJVMProjectSettings.enableBuildWithJps
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.copy(enableBuildWithJps = isSelected)
      }
    }

  private fun initHotSwapEnabledCheckBox(): JBCheckBox =
    JBCheckBox(BazelPluginBundle.message("project.settings.plugin.hotswap.enabled.checkbox.text")).apply {
      isSelected = currentJVMProjectSettings.hotSwapEnabled
      addItemListener {
        currentJVMProjectSettings = currentJVMProjectSettings.withNewHotSwapEnabled(isSelected)
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
}
