package org.jetbrains.bazel.run.config

import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.execution.ui.RunnerAndConfigurationAwareSettingsEditor
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.options.SettingsEditorListener
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.panels.Wrapper
import org.jetbrains.bazel.run.BazelRunHandler
import javax.swing.JComponent

/**
 * Delegates to [BazelRunConfigurationEditor] and recreates it whenever [BazelRunHandler] changes.
 *
 * Code based on [com.intellij.execution.ui.RunnerAndConfigurationSettingsEditor] which also deals with [SettingsEditor] delegation.
 */
internal class BazelRunConfigurationDynamicEditor(
  private val runConfiguration: BazelRunConfiguration,
) : SettingsEditor<BazelRunConfiguration>(), RunnerAndConfigurationAwareSettingsEditor {
  private var delegate: BazelRunConfigurationEditor
  private var runnerAndConfigurationSettings: RunnerAndConfigurationSettingsImpl? = null

  init {
    delegate = BazelRunConfigurationEditor(runConfiguration)
    registerDelegateListeners()
  }

  private fun registerDelegateListeners() {
    Disposer.register(this, delegate)
    delegate.addSettingsEditorListener(SettingsEditorListener { fireEditorStateChanged() })
  }

  override fun resetEditorFrom(s: BazelRunConfiguration) {
    delegate.resetEditorFrom(s)
  }

  override fun applyEditorTo(s: BazelRunConfiguration) {
    delegate.applyEditorTo(s)
  }

  override fun createEditor(): JComponent {
    val wrapper = Wrapper(delegate.component)
    val listener = object : BazelRunConfiguration.BazelRunHandlerChangedListener {
      override fun onRunHandlerChanged(newRunHandler: BazelRunHandler) {
        if (delegate.handler != newRunHandler) {
          runnerAndConfigurationSettings?.let { delegate.applyEditorTo(it) }
          delegate.applyTo(runConfiguration)
          Disposer.dispose(delegate)

          delegate = BazelRunConfigurationEditor(runConfiguration)
          registerDelegateListeners()
          runnerAndConfigurationSettings?.let { delegate.resetEditorFrom(it) }
          delegate.resetFrom(runConfiguration)
          wrapper.setContent(delegate.component)
        }
      }
    }
    runConfiguration.addOnRunHandlerChangedListener(listener, this)
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
