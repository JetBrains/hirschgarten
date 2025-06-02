/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.run2.state

import com.google.common.base.Strings
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import org.jdom.Element
import javax.swing.JComponent

/** State for a Blaze binary to run configurations with.  */
class BlazeBinaryState : RunConfigurationState {
  @JvmField
  var blazeBinary: String? = null

  override fun readExternal(element: Element) {
    blazeBinary = element.getAttributeValue(BLAZE_BINARY_ATTR)
  }

  override fun writeExternal(element: Element) {
    if (!Strings.isNullOrEmpty(blazeBinary)) {
      element.setAttribute(BLAZE_BINARY_ATTR, blazeBinary)
    } else {
      element.removeAttribute(BLAZE_BINARY_ATTR)
    }
  }

  override fun getEditor(project: Project): RunConfigurationStateEditor = BlazeBinaryStateEditor(project)

  private class BlazeBinaryStateEditor(project: Project) : RunConfigurationStateEditor {
    private val blazeBinaryField = JBTextField(1)

    init {
      blazeBinaryField.emptyText.text = "(Use global)"
    }

    override fun setComponentEnabled(enabled: Boolean) {
      blazeBinaryField.setEnabled(enabled)
    }

    override fun resetEditorFrom(genericState: RunConfigurationState) {
      val state = genericState as BlazeBinaryState
      blazeBinaryField.setText(Strings.nullToEmpty(state.blazeBinary))
    }

    override fun applyEditorTo(genericState: RunConfigurationState) {
      val state = genericState as BlazeBinaryState
      state.blazeBinary = Strings.emptyToNull(blazeBinaryField.getText())
    }

    override fun createComponent(): JComponent = UiUtil.createBox(JBLabel("Bazel" + " binary:"), blazeBinaryField)
  }

  companion object {
    private const val BLAZE_BINARY_ATTR = "blaze-binary"
  }
}
