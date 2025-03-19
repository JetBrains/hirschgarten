package org.jetbrains.bazel.ui.dialogs.queryTab

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.CollapsiblePanel
import com.intellij.ui.LanguageTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.utils.BazelWorkingDirectoryManager
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

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

  val isSelected get() = checkBox.isSelected
}

// TODO: Rename to something else than window and move to correct directory
class BazelQueryDialogWindow(private val project: Project) : JPanel() {
  private val defaultFlags = listOf(
    QueryFlagField("noimplicit_deps"),
    QueryFlagField("flag2"),
    QueryFlagField("flag3"),
  )

  // Bazel Runner
  private val queryEvaluator = QueryEvaluator()

  // UI elements
  private val editorTextField = LanguageTextField(BazelqueryLanguage, project, "")
  private val resultField = JBTextArea().apply { isEditable = false }
  private val directoryField = JBTextField().apply { isEditable = false }
  private val flagsPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    defaultFlags.forEach { it.addToPanel(this) }
  }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    chooseDirectory(project.baseDir)
    initializeUI()
  }

  private fun initializeUI() {
    fun createDirectorySelectionPanel() = JPanel(BorderLayout()).apply {
      val directoryButton = JButton("Select").apply { addActionListener { chooseDirectory() } }

      add(JLabel("Selected Directory: "), BorderLayout.WEST)
      add(directoryField, BorderLayout.CENTER)
      add(directoryButton, BorderLayout.EAST)
    }

    fun createQueryPanel() = JPanel(BorderLayout()).apply {
      add(editorTextField, BorderLayout.CENTER)
    }

    fun createFlagsPanel() = CollapsiblePanel(
      flagsPanel,
      true,
      true,
      AllIcons.General.ChevronUp,
      AllIcons.General.ChevronDown,
      "Flags"
    )

    fun createResultPanel() = JPanel(BorderLayout()).apply {
      add(JBScrollPane(resultField).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
      })
    }

    fun createButtonsPanel() = JPanel(BorderLayout()).apply {
      val evaluateButton = JButton("Evaluate").apply { addActionListener { evaluate() } }
      add(evaluateButton, BorderLayout.CENTER)
    }

    add(createDirectorySelectionPanel())
    add(createQueryPanel())
    add(createFlagsPanel())
    add(createResultPanel())
    add(createButtonsPanel())
  }

  // Directory selection
  private fun chooseDirectory(dirFile: VirtualFile? = null) {
    // If argument not passed (or passed as null) display window for user to choose from:
    val chosenDir = if (dirFile != null) {
      if (!dirFile.isDirectory) throw IllegalArgumentException("$dirFile is not a directory")
      dirFile
    } else {
      val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
        title = "Select Directory"
        description = "Choose a directory within the project."
      }
      FileChooser.chooseFile(descriptor, project, null)
    }

    if (chosenDir != null) {
      val relativePath = VfsUtilCore.getRelativePath(
        chosenDir,
        project.baseDir ?: chosenDir,
        '/'
      )

      if (relativePath == null) {
        if (dirFile != null) throw IllegalArgumentException("$dirFile is not in project")

        NotificationGroupManager.getInstance()
          .getNotificationGroup("Bazel")
          .createNotification("Selected directory is outside of current project", NotificationType.ERROR)
          .notify(project)
        return
      }

      directoryField.text = "//$relativePath"
      BazelWorkingDirectoryManager.setWorkingDirectory(chosenDir.path)
      queryEvaluator.setEvaluationDirectory(chosenDir)
    }

  }

  private fun evaluate() {
    if (!queryEvaluator.isDirectorySet) {
      System.err.println("Bazel Query directory not set or other error occurred")
      resultField.text = "Bazel Query directory not set or other error occurred"
      updateUI()
      return
    }

    val flagsToRun = mutableListOf<String>()
    for (flag in defaultFlags) {
      if (flag.isSelected) {
        flagsToRun.add(flag.flag)
      }
    }

    val commandResults = queryEvaluator.evaluate(editorTextField.text, flagsToRun)

    if (commandResults.isSuccess) {
      resultField.text = commandResults.stdout.ifEmpty { "Nothing found" }
    } else {
      resultField.text = "Command execution failed:\n" + commandResults.stderr
    }
    updateUI()
  }

  fun clear() {
    editorTextField.text = ""
    resultField.text = ""
  }
}
