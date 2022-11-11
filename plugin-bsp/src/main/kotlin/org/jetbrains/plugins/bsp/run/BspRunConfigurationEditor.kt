package org.jetbrains.plugins.bsp.run

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
  private var runType: LabeledComponent<ComboBox<BspRunType>> =
    LabeledComponent.create(ComboBox(BspRunType.values()), "Run type")

  override fun resetEditorFrom(demoRunConfiguration: BspRunConfiguration) {
    targetName.component?.setText(demoRunConfiguration.target)
    runType.component?.selectedItem = demoRunConfiguration.runType
  }

  override fun applyEditorTo(demoRunConfiguration: BspRunConfiguration) {
    targetName.component?.let { demoRunConfiguration.target = it.text }
    runType.component?.let { demoRunConfiguration.runType = it.selectedItem as BspRunType }
  }

  override fun createEditor(): JComponent = panel {
    row {
      cell(runType)
      cell(targetName)
    }
  }.also { myPanel = it }
}
