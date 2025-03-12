/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.ogRun.state

import com.google.idea.blaze.base.ui.UiUtil
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBCheckBox
import java.awt.event.ActionListener
import kotlin.concurrent.Volatile

/**
 * Optionally save console output to a file. All the state / serialization code is handled upstream,
 * this class just displays the UI elements.
 */
class ConsoleOutputFileSettingsUi<T : RunConfigurationBase<*>?>
  : SettingsEditor<T?>() {
  private val saveToFile = JBCheckBox("Save console output to file:",  /* selected= */false)
  private val outputFile = TextFieldWithBrowseButton()

  @Volatile
  private var uiEnabled = true

  init {
    outputFile.addBrowseFolderListener(
      "Choose File to Save Console Output",
      "Console output would be saved to the specified file",  /* project= */
      null,
      FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor(),
      TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT,
    )
    saveToFile.addActionListener(ActionListener { e: ActionEvent? -> outputFile.setEnabled(uiEnabled && saveToFile.isSelected()) })
  }

  public override fun resetEditorFrom(config: T?) {
    saveToFile.setSelected(config!!.isSaveOutputToFile())
    val fileOutputPath = config.getOutputFilePath()
    outputFile.setText(
      if (fileOutputPath == null) "" else FileUtil.toSystemDependentName(fileOutputPath),
    )
  }

  public override fun applyEditorTo(config: T?) {
    val text = outputFile.getText()
    config!!.setFileOutputPath(
      if (StringUtil.isEmpty(text)) null else FileUtil.toSystemIndependentName(text),
    )
    config.setSaveOutputToFile(saveToFile.isSelected())
  }

  override fun createEditor(): JComponent {
    return UiUtil.createHorizontalBox( /* gap= */5, saveToFile, outputFile)
  }

  fun setComponentEnabled(componentEnabled: Boolean) {
    uiEnabled = componentEnabled
    updateVisibleAndEnabled()
  }

  private fun updateVisibleAndEnabled() {
    if (!enabled.getValue()) {
      saveToFile.setSelected(false)
      saveToFile.setVisible(false)
      outputFile.setVisible(false)
      return
    }
    saveToFile.setEnabled(uiEnabled)
    outputFile.setEnabled(uiEnabled && saveToFile.isSelected())
  }

  companion object {
    private val enabled: BoolExperiment = BoolExperiment("save.to.file.enabled", true)
  }
}
