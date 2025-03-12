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
package org.jetbrains.bazel.ogRun.state

import com.google.common.base.Strings
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import org.jdom.Element
import org.jetbrains.bazel.ogRun.other.BlazeCommandName

/** State for a [BlazeCommandName].  */
class BlazeCommandState : RunConfigurationState {
  private var command: BlazeCommandName? = null

  fun getCommand(): BlazeCommandName? = command

  fun setCommand(command: BlazeCommandName?) {
    this.command = command
  }

  override fun readExternal(element: Element) {
    val commandString = element.getAttributeValue(COMMAND_ATTR)
    command =
      if (Strings.isNullOrEmpty(commandString)) null else BlazeCommandName.fromString(commandString)
  }

  override fun writeExternal(element: Element) {
    if (command != null) {
      element.setAttribute(COMMAND_ATTR, command.toString())
    } else {
      element.removeAttribute(COMMAND_ATTR)
    }
  }

  override fun getEditor(project: Project?): RunConfigurationStateEditor = BlazeCommandStateEditor(project)

  private class BlazeCommandStateEditor(project: Project?) : RunConfigurationStateEditor {
    private val buildSystemName: String?

    private val commandCombo: ComboBox<*>

    init {
      buildSystemName = Blaze.buildSystemName(project)
      commandCombo =
        ComboBox<Any?>(DefaultComboBoxModel<Any?>(BlazeCommandName.knownCommands().toArray()))
      // Allow the user to manually specify an unlisted command.
      commandCombo.setEditable(true)
    }

    override fun setComponentEnabled(enabled: Boolean) {
      commandCombo.setEnabled(enabled)
    }

    override fun resetEditorFrom(genericState: RunConfigurationState?) {
      val state = genericState as BlazeCommandState
      commandCombo.setSelectedItem(state.getCommand())
    }

    override fun applyEditorTo(genericState: RunConfigurationState?) {
      val state = genericState as BlazeCommandState
      val selectedCommand = commandCombo.getSelectedItem()
      if (selectedCommand is BlazeCommandName) {
        state.setCommand(selectedCommand as BlazeCommandName?)
      } else {
        state.setCommand(
          if (Strings.isNullOrEmpty(selectedCommand as String?)) {
            null
          } else {
            BlazeCommandName.fromString(selectedCommand.toString())
          },
        )
      }
    }

    override fun createComponent(): JComponent = UiUtil.createBox(JLabel(buildSystemName + " command:"), commandCombo)
  }

  companion object {
    private const val COMMAND_ATTR = "blaze-command"
  }
}
