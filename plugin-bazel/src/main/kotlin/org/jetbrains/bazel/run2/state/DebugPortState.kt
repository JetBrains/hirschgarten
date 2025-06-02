/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.ui.PortField
import com.intellij.util.ui.FormBuilder
import org.jdom.Element
import javax.swing.JComponent

/** User-defined debug port.  */
class DebugPortState(private val defaultPort: Int) : RunConfigurationState {
  var port: Int = defaultPort

  @Throws(InvalidDataException::class)
  override fun readExternal(element: Element) {
    val value = element.getAttributeValue(ATTRIBUTE_TAG)
    if (value == null) {
      return
    }
    try {
      port = value.toInt()
    } catch (e: NumberFormatException) {
      // ignore
    }
  }

  @Throws(WriteExternalException::class)
  override fun writeExternal(element: Element) {
    if (port != defaultPort) {
      element.setAttribute(ATTRIBUTE_TAG, port.toString())
    } else {
      element.removeAttribute(ATTRIBUTE_TAG)
    }
  }

  override fun getEditor(project: Project): RunConfigurationStateEditor = DebugPortState.Editor()

  private inner class Editor : RunConfigurationStateEditor {
    private val portField = PortField(defaultPort)

    override fun resetEditorFrom(genericState: RunConfigurationState) {
      val state = genericState as DebugPortState
      portField.number = state.port
    }

    override fun applyEditorTo(genericState: RunConfigurationState) {
      val state = genericState as DebugPortState
      state.port = portField.number
    }

    override fun createComponent(): JComponent = FormBuilder.createFormBuilder().addLabeledComponent("&Port:", portField).getPanel()

    override fun setComponentEnabled(enabled: Boolean) {
      portField.setEnabled(enabled)
    }
  }

  companion object {
    private const val ATTRIBUTE_TAG = "debug_port"
  }
}
