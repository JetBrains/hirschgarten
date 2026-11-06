package org.jetbrains.bazel.run.config

import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.ui.RunnerAndConfigurationAwareSettingsEditor
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorListener
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.panels.Wrapper
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import javax.swing.JComponent

/**
 * Delegates to [BazelRunConfigurationEditor] and recreates it when the targets or [BazelRunHandler] change.
 *
 * Code based on [com.intellij.execution.ui.RunnerAndConfigurationSettingsEditor] which also deals with [SettingsEditor] delegation.
 */
internal class BazelRunConfigurationDynamicEditor(
  runConfiguration: BazelRunConfiguration,
) : SettingsEditor<BazelRunConfiguration>(), RunnerAndConfigurationAwareSettingsEditor {
  private var delegate: BazelRunConfigurationEditor
  private var delegateHandlerProvider: RunHandlerProvider? = null
  private var delegateTargets: List<Label> = emptyList()
  private var runnerAndConfigurationSettings: RunnerAndConfigurationSettingsImpl? = null
  private val wrapper = Wrapper()

  init {
    delegate = createDelegate(runConfiguration)
  }

  private fun createDelegate(configuration: BazelRunConfiguration): BazelRunConfigurationEditor {
    val newDelegate = BazelRunConfigurationEditor(configuration)
    delegateHandlerProvider = configuration.handlerProvider
    delegateTargets = configuration.targets.toList()
    Disposer.register(this, newDelegate)
    newDelegate.addSettingsEditorListener(SettingsEditorListener { fireEditorStateChanged() })
    return newDelegate
  }

  private fun recreateDelegate(configuration: BazelRunConfiguration, settings: RunnerAndConfigurationSettingsImpl?) {
    Disposer.dispose(delegate)
    delegate = createDelegate(configuration)
    settings?.let { delegate.resetEditorFrom(it) }
    delegate.resetFrom(configuration)
    wrapper.setContent(delegate.component)
  }

  override fun resetEditorFrom(s: BazelRunConfiguration) {
    if (s.handlerProvider == delegateHandlerProvider && s.targets == delegateTargets) {
      delegate.resetEditorFrom(s)
    } else {
      recreateDelegate(s, runnerAndConfigurationSettings)
    }
  }

  override fun applyEditorTo(s: BazelRunConfiguration) {
    delegate.applyEditorTo(s)
    if (s.handlerProvider == delegateHandlerProvider && s.targets == delegateTargets) return
    val focusedFragmentId = delegate.findFocusedFragmentId()
    val settingsCopy = runnerAndConfigurationSettings?.clone()
    settingsCopy?.let { delegate.applyEditorTo(it) }
    recreateDelegate(s.clone() as BazelRunConfiguration, settingsCopy)
    focusedFragmentId?.let { delegate.focusFragment(it) }
  }

  override fun createEditor(): JComponent {
    wrapper.setContent(delegate.component)
    return wrapper
  }

  override fun disposeEditor() {
    delegate.disposeEditor()
  }

  override fun getSnapshot(): BazelRunConfiguration {
    return delegate.getSnapshot()
  }

  override fun isSpecificallyModified(): Boolean {
    return delegate.isSpecificallyModified
  }

  override fun isReadyForApply(): Boolean {
    return delegate.isReadyForApply
  }

  override fun resetEditorFrom(s: RunnerAndConfigurationSettingsImpl) {
    runnerAndConfigurationSettings = s
    delegate.resetEditorFrom(s)
  }

  override fun applyEditorTo(s: RunnerAndConfigurationSettingsImpl) {
    delegate.applyEditorTo(s)
  }

  override fun targetChanged(targetName: String?) {
    delegate.targetChanged(targetName)
  }

  override fun isInplaceValidationSupported(): Boolean {
    return delegate.isInplaceValidationSupported()
  }
}
