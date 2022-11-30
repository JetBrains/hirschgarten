package org.jetbrains.plugins.bsp.runConfig

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JPanel

public class BspRunConfigurationEditor(project: Project) : SettingsEditor<BspRunConfiguration>() {

  private lateinit var myPanel: JPanel
  private var targetName: LabeledComponent<TextFieldWithBrowseButton> =
    LabeledComponent.create(TextFieldWithBrowseButton(), "Target")

  override fun resetEditorFrom(demoRunConfiguration: BspRunConfiguration) {
    targetName.component?.setText(demoRunConfiguration.state?.target)
  }

  override fun applyEditorTo(demoRunConfiguration: BspRunConfiguration) {
    targetName.component?.let { demoRunConfiguration.state?.target = it.text }
  }

  override fun createEditor(): JComponent = panel {
    row {
      cell(targetName)
    }
  }.also { myPanel = it }
}
