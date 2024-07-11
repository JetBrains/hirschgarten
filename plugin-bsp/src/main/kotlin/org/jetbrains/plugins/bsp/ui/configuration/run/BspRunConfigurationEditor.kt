package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.compiler.options.CompileStepBeforeRun
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.ui.BeforeRunFragment
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.CommonTags
import com.intellij.execution.ui.RunConfigurationFragmentedEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.addBeforeRunFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.addEnvironmentFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.addLabeledSettingsEditorFragment
import com.intellij.openapi.externalSystem.service.ui.util.LabeledSettingsFragmentInfo
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase

public class BspRunConfigurationEditor(private val runConfiguration: BspRunConfigurationBase) :
  RunConfigurationFragmentedEditor<BspRunConfigurationBase>(
    runConfiguration, BspRunConfigurationExtensionManager.getInstance()
  ) {
  override fun createRunFragments(): List<SettingsEditorFragment<BspRunConfigurationBase, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonParameterFragments.createRunHeader())
      addBeforeRunFragment(CompileStepBeforeRun.ID)
      addAll(BeforeRunFragment.createGroup())
      add(CommonTags.parallelRun())
      addBspTargetFragment()
      addBspEnvironmentFragment()
    }

  private fun SettingsEditorFragmentContainer<BspRunConfigurationBase>.addBspEnvironmentFragment() {
    this.addEnvironmentFragment(object : LabeledSettingsFragmentInfo {
      override val editorLabel: String = ExecutionBundle.message("environment.variables.component.title")
      override val settingsId: String = "external.system.environment.variables.fragment" // TODO: does it matter?
      override val settingsName: String = ExecutionBundle.message("environment.variables.fragment.name")
      override val settingsGroup: String = ExecutionBundle.message("group.operating.system")
      override val settingsHint: String = ExecutionBundle.message("environment.variables.fragment.hint")
      override val settingsActionHint: String =
        ExecutionBundle.message("set.custom.environment.variables.for.the.process")
    },
      { runConfiguration.env.envs },
      { runConfiguration.env.with(it) },
      { runConfiguration.env.isPassParentEnvs },
      { runConfiguration.env.with(it) },
      false
    )
  }

  private fun SettingsEditorFragmentContainer<BspRunConfigurationBase>.addBspTargetFragment() {
    this.addLabeledSettingsEditorFragment(object : LabeledSettingsFragmentInfo { // TODO: Use bundle
      override val editorLabel: String = "Build target"
      override val settingsId: String = "bsp.target.fragment"
      override val settingsName: String = "Build target"
      override val settingsGroup: String = "BSP"
      override val settingsHint: String = "Build target"
      override val settingsActionHint: String = "Build target"
    }, { BspTargetBrowserComponent() }, { s, c ->
      c.text = s.targets.singleOrNull()?.id ?: ""
    }, { _, _ ->
      // TODO: set target
    }, { true })
  }
}

public class BspTargetBrowserComponent : TextFieldWithBrowseButton() {
  // TODO: implement a browser
}
