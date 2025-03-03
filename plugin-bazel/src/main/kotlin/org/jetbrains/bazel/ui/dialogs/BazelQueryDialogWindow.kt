package org.jetbrains.bazel.ui.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CollapsiblePanel
import com.intellij.ui.EditorTextField
import com.intellij.ui.RawCommandLineEditor
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFileType


class BazelQueryDialogWindow(private val project: Project) : DialogWrapper(true) {
  private class QueryFlagField(
    val flag: String,
    checked: Boolean = false,
    val removable: Boolean = false
  ) {
    class CheckListener(val checkBox: JCheckBox, var wasSelected: Boolean) : ActionListener {
      override fun actionPerformed(e: ActionEvent?) {
        if (checkBox.isSelected && !wasSelected) {
          selectedFlagCount--
        }

        if (!checkBox.isSelected && wasSelected) {
          selectedFlagCount--
        }

        wasSelected = checkBox.isSelected
      }
    }

    val checkBox = JCheckBox(flag, checked)


    init {
      checkBox.addActionListener(CheckListener(checkBox, checkBox.isSelected))
    }

    fun addToPanel(panel: JPanel) {
      panel.add(checkBox)
    }

    fun isSelected() = checkBox.isSelected
  }

  val editorTextField = EditorTextField(
    EditorFactory.getInstance().createDocument(""),
    project,
    BazelqueryFileType,
    false,
    true
  )

  private companion object {
    private val defaultFlags = listOf(
      QueryFlagField("flag1"),
      QueryFlagField("flag2"),
      QueryFlagField("flag3"),
    )

    val commandField = RawCommandLineEditor()
    val resultField = JTextPane()

    var selectedFlagCount = 0
    val flagsPanel = JPanel()
    val flagsPanelHolder = CollapsiblePanel(
      flagsPanel, true, true,
      AllIcons.General.ChevronUp, AllIcons.General.ChevronDown,
      "Flags"
    )

    init {
      resultField.isEditable = false

      flagsPanel.layout = BoxLayout(flagsPanel, BoxLayout.Y_AXIS)
      for (flag in defaultFlags) {
        flag.addToPanel(flagsPanel)
      }
    }
  }

  init {
    title = "Bazel Query"
    flagsPanelHolder.collapse()
    init()
  }

  private fun clear() {
    editorTextField.text = ""
    resultField.text = ""
    flagsPanel.updateUI()
  }

  override fun createCenterPanel(): JComponent? {
    val dialogPanel = JPanel()

    val queryPanel = JPanel(BorderLayout())
    val quoteLabelL = JLabel("\"")
    val quoteLabelR = JLabel("\"")
    queryPanel.add(quoteLabelL, BorderLayout.EAST)
    queryPanel.add(quoteLabelR, BorderLayout.WEST)
    queryPanel.add(editorTextField, BorderLayout.CENTER)


    dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)
    dialogPanel.add(queryPanel)
    dialogPanel.add(flagsPanelHolder)
    dialogPanel.add(resultField)

    return dialogPanel
  }

  override fun createActions(): Array<out Action?> {
    class EvalAction : DialogWrapperAction("Evaluate") {
      init {
        putValue(DEFAULT_ACTION, true)
      }

      override fun doAction(e: ActionEvent?) {
        evaluate()
      }
    }

    class ClearAction : DialogWrapperAction("Clear") {

      override fun doAction(e: ActionEvent?) {
        clear()
      }
    }

    return arrayOf(EvalAction(), ClearAction())
  }

  override fun pack() {
    println(":D")
  }

  private fun evaluate() {
    val resultTextBuilder = StringBuilder("bazel query \"" + editorTextField.text + "\"")
    for (flag in defaultFlags) {
      if (flag.isSelected()) resultTextBuilder.append(" --").append(flag.flag)
    }
    resultField.text = resultTextBuilder.toString()
  }
}
