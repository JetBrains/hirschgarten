package org.jetbrains.bazel.ui.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollapsiblePanel
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBTextField
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
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsLanguage
import org.jetbrains.bazel.utils.BazelWorkingDirectoryManager
import javax.swing.JButton


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

  val editorTextField = LanguageTextField(
    BazelqueryLanguage,
    project,
    ""
  )

  val flagTextField = LanguageTextField(
    BazelqueryFlagsLanguage,
    project,
    ""
  )

  private companion object {
    private val defaultFlags = listOf(
      QueryFlagField("flag1"),
      QueryFlagField("flag2"),
      QueryFlagField("flag3"),
    )
    val resultField = JTextPane()
    private val directoryField: JBTextField = JBTextField()
    private val directoryButton = JButton("Select")

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
    setupDirectorySelection()
    init()
  }

  private fun setupDirectorySelection() {
    directoryButton.addActionListener {
      val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
      descriptor.title = "Select Directory"
      descriptor.description = "Choose a directory within the project."

      val chosenDir: VirtualFile? = FileChooser.chooseFile(descriptor, project, null)
      if (chosenDir != null) {
        val relativePath = VfsUtilCore.getRelativePath(chosenDir, project.baseDir ?: chosenDir, if (SystemInfo.isWindows) '\\' else '/')
        directoryField.text = relativePath
        BazelWorkingDirectoryManager.setWorkingDirectory(chosenDir.path)
      }
    }
  }


  private fun clear() {
    editorTextField.text = ""
    flagTextField.text = ""
    resultField.text = ""
    flagsPanel.updateUI()
  }

  override fun createCenterPanel(): JComponent? {
    val dialogPanel = JPanel()

    val directorySelectionPanel = JPanel(BorderLayout())
    directorySelectionPanel.add(JLabel("Selected Directory: "), BorderLayout.WEST)
    directorySelectionPanel.add(directoryField, BorderLayout.CENTER)
    directorySelectionPanel.add(directoryButton, BorderLayout.EAST)

    val queryPanel = JPanel(BorderLayout())
    queryPanel.add(editorTextField, BorderLayout.CENTER)

    dialogPanel.layout = BoxLayout(dialogPanel, BoxLayout.Y_AXIS)
    dialogPanel.add(directorySelectionPanel)
    dialogPanel.add(queryPanel)
    dialogPanel.add(flagsPanelHolder)
    dialogPanel.add(flagTextField)
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
    val queryQuotes = if (editorTextField.text.contains("\"")) "'" else "\""
    val resultTextBuilder = StringBuilder("bazel query $queryQuotes${editorTextField.text}$queryQuotes")
    for (flag in defaultFlags) {
      if (flag.isSelected()) resultTextBuilder.append(" --").append(flag.flag)
    }
    resultTextBuilder.append(" ").append(flagTextField.text)
    resultField.text = resultTextBuilder.toString()
  }
}
