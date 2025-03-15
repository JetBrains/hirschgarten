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

import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Joiner

import com.google.idea.blaze.base.command.BlazeFlags
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.execution.ParametersListUtil
import org.jdom.Element
import org.jetbrains.bazel.ogRun.other.UiUtil
import java.awt.Container
import java.awt.Dimension
import java.util.stream.Collectors
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JTextArea
import javax.swing.JViewport
import javax.swing.ScrollPaneConstants
import javax.swing.ViewportLayout

/** State for a list of user-defined flags.  */
class RunConfigurationFlagsState(private val tag: String?, private val fieldLabel: String?) : RunConfigurationState {
  /** Unprocessed flags, as the user entered them, tokenised on unquoted whitespace.  */
  private var flags: List<String?> = listOf<String?>()

  val flagsForExternalProcesses: MutableList<String?>
    /** Flags ready to be used directly as args for external processes.  */
    get() {
      val processedFlags =
        flags
          .stream()
          .map { s: String? ->
            ParametersListUtil
              .parse(
                s!!,
                false,
                true,
              ).first()
          }.collect(Collectors.toList())
      return BlazeFlags.expandBuildFlags(processedFlags)
    }

  var rawFlags: MutableList<String?>
    /** Unprocessed flags that haven't been macro expanded or processed for escaping/quotes.  */
    get() = flags
    set(flags) {
      this.flags = listOf<String?>(flags)
    }

  fun copy(): RunConfigurationFlagsState {
    val state = RunConfigurationFlagsState(tag, fieldLabel)
    state.rawFlags = this.rawFlags
    return state
  }

  override fun readExternal(element: Element) {
    val flagsBuilder = List.builder<String?>()
    for (e in element.getChildren(tag)) {
      val flag = e.textTrim
      if (flag != null && !flag.isEmpty()) {
        flagsBuilder.add(flag)
      }
    }
    flags = flagsBuilder.build()
  }

  override fun writeExternal(element: Element) {
    element.removeChildren(tag)
    for (flag in flags) {
      val child = Element(tag)
      child.setText(flag)
      element.addContent(child)
    }
  }

  override fun getEditor(project: Project): RunConfigurationStateEditor = RunConfigurationFlagsStateEditor(fieldLabel)

  /** Editor component for flags list  */
  protected class RunConfigurationFlagsStateEditor internal constructor(private val fieldLabel: String?) : RunConfigurationStateEditor {
    private val flagsField: JTextArea

    init {
      flagsField = createFlagsField()
    }

    private fun createFlagsField(): JTextArea {
      val field: JTextArea =
        object : JTextArea() {
          val minimumSize: Dimension
            get() = // Jetbrains' DefaultScrollBarUI will automatically hide the scrollbar knob
              // if the viewport height is less than twice the scrollbar's width.
              // In the default font, 2 rows is slightly taller than this, guaranteeing
              // that the scrollbar knob is visible when the field is scrollable.
              Dimension(getColumnWidth(), 2 * getRowHeight())
        }
      // This is the preferred number of rows. The field will grow if there is more text,
      // and shrink if there is not enough room in the dialog.
      field.setRows(5)
      field.setLineWrap(true)
      field.setWrapStyleWord(true)
      return field
    }

    override fun setComponentEnabled(enabled: Boolean) {
      flagsField.setEnabled(enabled)
    }

    override fun resetEditorFrom(genericState: RunConfigurationState) {
      val state = genericState as RunConfigurationFlagsState
      // join on newline chars only, otherwise leave unchanged
      flagsField.setText(Joiner.on('\n').join(state.rawFlags))
    }

    override fun applyEditorTo(genericState: RunConfigurationState) {
      val state = genericState as RunConfigurationFlagsState
      // split on unescaped whitespace and newlines only. Otherwise leave unchanged.
      val list: MutableList<String?> = BlazeParametersListUtil.splitParameters(flagsField.getText())
      state.rawFlags = list
    }

    private fun createScrollPane(field: JTextArea): JBScrollPane {
      val viewport: JViewport = JViewport()
      viewport.setView(field)
      viewport.setLayout(
        object : ViewportLayout() {
          override fun preferredLayoutSize(parent: Container?): Dimension? = field.getPreferredSize()

          override fun minimumLayoutSize(parent: Container?): Dimension? = field.getMinimumSize()
        },
      )

      val scrollPane =
        JBScrollPane(
          ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
        )
      scrollPane.setViewport(viewport)
      return scrollPane
    }

    override fun createComponent(): JComponent = UiUtil.createBox(JLabel(fieldLabel), createScrollPane(flagsField))

    @get:VisibleForTesting
    val internalComponent: JComponent
      get() = flagsField
  }
}
