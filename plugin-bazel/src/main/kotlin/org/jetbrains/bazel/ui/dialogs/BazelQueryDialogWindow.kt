package org.jetbrains.bazel.ui.dialogs

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollapsiblePanel
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBTextField
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsLanguage
import org.jetbrains.bazel.utils.BazelWorkingDirectoryManager
import java.awt.BorderLayout
import javax.swing.*

class BazelQueryDialogWindow(private val project: Project) : JPanel() {

  // Flags
  private class QueryFlagField(
    val flag: String,
    checked: Boolean = false
  ) {
    val checkBox = JCheckBox(flag, checked).apply {
      addActionListener { updateFlagCount(isSelected) }
    }

    companion object {
      var selectedFlagCount = 0

      private fun updateFlagCount(isSelected: Boolean) {
        selectedFlagCount += if (isSelected) 1 else -1
      }
    }

    fun addToPanel(panel: JPanel) {
      panel.add(checkBox)
    }

    fun isSelected() = checkBox.isSelected
  }
  private val defaultFlags = listOf(
    QueryFlagField("flag1"),
    QueryFlagField("flag2"),
    QueryFlagField("flag3"),
  )
  private val flagsPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    defaultFlags.forEach { it.addToPanel(this) }
  }

  // UI elements
  private val editorTextField = createLanguageTextField(BazelqueryLanguage)
  private val flagTextField = createLanguageTextField(BazelqueryFlagsLanguage)
  private val resultField = JTextPane().apply { isEditable = false }
  private val directoryField = JBTextField()
  private val directoryButton = JButton("Select").apply { addActionListener { chooseDirectory() } }
  private val flagsPanelHolder = CollapsiblePanel(
    flagsPanel,
    true,
    true,
    AllIcons.General.ChevronUp,
    AllIcons.General.ChevronDown,
    "Flags"
  )

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    initializeUI()
  }

  private fun initializeUI() {
    add(createDirectorySelectionPanel())
    add(createQueryPanel())
    add(flagsPanelHolder)
    add(flagTextField)
    add(resultField)
    add(createEvaluateButton())
  }

  private fun createDirectorySelectionPanel(): JPanel {
    return JPanel(BorderLayout()).apply {
      add(JLabel("Selected Directory: "), BorderLayout.WEST)
      add(directoryField, BorderLayout.CENTER)
      add(directoryButton, BorderLayout.EAST)
    }
  }

  // Directory selection
  private fun chooseDirectory() {
    val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
      title = "Select Directory"
      description = "Choose a directory within the project."
    }

    val chosenDir: VirtualFile? = FileChooser.chooseFile(descriptor, project, null)
    if (chosenDir != null) {
      val relativePath = VfsUtilCore.getRelativePath(
        chosenDir,
        project.baseDir ?: chosenDir,
        if (SystemInfo.isWindows) '\\' else '/'
      )
      directoryField.text = relativePath
      BazelWorkingDirectoryManager.setWorkingDirectory(chosenDir.path)
    }
  }

  // Query panel
  private fun createQueryPanel(): JPanel {
    return JPanel(BorderLayout()).apply {
      add(editorTextField, BorderLayout.CENTER)
    }
  }

  // Evaluate button
  private fun createEvaluateButton(): JPanel {
    val evaluateButton = JButton("Evaluate").apply { addActionListener { evaluate() } }
    return JPanel(BorderLayout()).apply {
      add(evaluateButton, BorderLayout.CENTER)
    }
  }

  private fun evaluate() {
    val queryQuotes = if (editorTextField.text.contains("\"")) "'" else "\""
    val resultTextBuilder = StringBuilder("bazel query $queryQuotes${editorTextField.text}$queryQuotes")

    defaultFlags
      .filter { it.isSelected() }
      .forEach { resultTextBuilder.append(" --").append(it.flag) }

    resultTextBuilder.append(" ").append(flagTextField.text)
    resultField.text = resultTextBuilder.toString()
  }

  private fun createLanguageTextField(language: com.intellij.lang.Language): LanguageTextField {
    return LanguageTextField(language, project, "")
  }

  fun clear() {
    editorTextField.text = ""
    flagTextField.text = ""
    resultField.text = ""
    flagsPanelHolder.updateUI()
  }
}
