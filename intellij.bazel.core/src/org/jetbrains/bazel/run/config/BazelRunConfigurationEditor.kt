package org.jetbrains.bazel.run.config

import com.intellij.execution.ui.BeforeRunComponent
import com.intellij.execution.ui.BeforeRunFragment
import com.intellij.execution.ui.CommonParameterFragments
import com.intellij.execution.ui.CommonTags
import com.intellij.execution.ui.RunConfigurationFragmentedEditor
import com.intellij.execution.ui.SettingsEditorFragment
import com.intellij.openapi.externalSystem.service.execution.configuration.fragments.SettingsEditorFragmentContainer
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.BazelRunHandler

/**
 * The base editor for a Bazel run configuration.
 * Takes care of targets, the common settings and sets up the handler-specific settings editor.
 * handler changes won't be affected in the UI until the editor is recreated
 */
@ApiStatus.Internal
class BazelRunConfigurationEditor(
  private val runConfiguration: BazelRunConfiguration,
  val handler: BazelRunHandler? = runConfiguration.handler,
) :
  RunConfigurationFragmentedEditor<BazelRunConfiguration>(runConfiguration, handler?.extensionsManager) {

  override fun initFragments(fragments: Collection<SettingsEditorFragment<BazelRunConfiguration, *>>) {
    super.initFragments(fragments)
    val allFragments = fragments.toList()
    BazelRunConfigurationEditorCustomizer.EP_NAME.forEachExtensionSafe { it.fragmentsCreated(runConfiguration, allFragments) }
  }

  override fun resetEditorFrom(s: BazelRunConfiguration) {
    super.resetEditorFrom(s)
    val allFragments = fragments.toList()
    BazelRunConfigurationEditorCustomizer.EP_NAME.forEachExtensionSafe { it.fragmentsReset(s, allFragments) }
  }

  override fun createRunFragments(): List<SettingsEditorFragment<BazelRunConfiguration, *>> =
    SettingsEditorFragmentContainer.fragments {
      add(CommonTags.parallelRun())
      addBeforeRunFragment()
      addTargetLabelsFragment()
      addStateEditorFragment()
    }

  private fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addBeforeRunFragment() {
    val beforeRunComponent = BeforeRunComponent(this@BazelRunConfigurationEditor)
    add(BeforeRunFragment.createBeforeRun(beforeRunComponent, null))
    addAll(BeforeRunFragment.createGroup())
  }

  private fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addStateEditorFragment() {
    val handler = handler ?: return
    this.add(CommonParameterFragments.createHeader(BazelPluginBundle.message("runconfig.header")))
    this.addAll(handler.state.createFragments(runConfiguration))
  }

  private fun SettingsEditorFragmentContainer<BazelRunConfiguration>.addTargetLabelsFragment() {
    add(TargetsFragment(project))
  }

  fun focusTargetLabelsFragment() {
    fragments.filterIsInstance<TargetsFragment>().forEach {
      it.editorComponent.requestFocus()
    }
  }
}
