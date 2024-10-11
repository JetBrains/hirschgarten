package org.jetbrains.bazel.debug.configuration

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import org.jetbrains.bazel.config.BazelPluginBundle
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class StarlarkDebugSettingsEditor : SettingsEditor<StarlarkDebugConfiguration>() {
  private val targetField = JTextField()
  private val targetLabel =
    createLabelFor(BazelPluginBundle.message("starlark.debug.config.target.label"), targetField)
  private val targetBox =
    JPanel().apply {
      add(targetLabel)
      add(targetField)
    }

  private fun createLabelFor(text: String?, component: JComponent): JLabel {
    val label = JLabel()
    LabeledComponent.TextWithMnemonic.fromTextWithMnemonic(text).setToLabel(label)
    label.labelFor = component
    return label
  }

  override fun resetEditorFrom(config: StarlarkDebugConfiguration) {
    targetField.text = config.getTarget()
  }

  override fun applyEditorTo(config: StarlarkDebugConfiguration) {
    config.setTarget(targetField.text)
  }

  override fun createEditor(): JComponent {
    val configPanel = JPanel(GridBagLayout())

    val constraints =
      GridBagConstraints().apply {
        gridy = 0
        anchor = GridBagConstraints.WEST
      }
    configPanel.add(targetBox, constraints)
    return configPanel
  }
}
