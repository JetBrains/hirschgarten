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
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.RelativeLabel
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.utils.BazelWorkingDirectoryManager
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.WorkspaceContextConstructor
import org.jetbrains.bazel.workspacecontext.WorkspaceContextProvider
import java.awt.BorderLayout
import java.nio.file.Path
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextPane

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
    QueryFlagField("noimplicit_deps"),
    QueryFlagField("flag2"),
    QueryFlagField("flag3"),
  )
  private val flagsPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    defaultFlags.forEach { it.addToPanel(this) }
  }

  // Bazel Runner
  var previousWorkspaceDir: VirtualFile? = null
  var bazelRunner: BazelRunner? = null

  // UI elements
  private val editorTextField = createLanguageTextField(BazelqueryLanguage)
  private val flagTextField = createLanguageTextField(BazelqueryFlagsLanguage)
  private val resultField = JTextPane().apply { isEditable = false }
  private val directoryField = JBTextField()
  private val directoryButton = JButton("Select").apply { addActionListener { chooseDirectory() } }
  private val clearButton = JButton("Clear").apply { addActionListener { clear() } }
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
    add(createClearButton())
    add(createDirectorySelectionPanel())
    add(createQueryPanel())
    add(flagsPanelHolder)
    add(flagTextField)
    add(resultField)
    add(createEvaluateButton())
  }

  private fun createClearButton() = JPanel(BorderLayout()).apply {
    add(clearButton, BorderLayout.WEST)
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

      if (chosenDir != previousWorkspaceDir) {
        previousWorkspaceDir = chosenDir

        val wcc = WorkspaceContextConstructor(chosenDir.toNioPath(), Path.of(""))
        val pv = org.jetbrains.bazel.projectview.model.ProjectView.Builder().build()
        val wcp =
          object : WorkspaceContextProvider {
            private val wc = wcc.construct(pv)
            override fun currentWorkspaceContext(): WorkspaceContext = wc
          }
        bazelRunner = BazelRunner(wcp, null, chosenDir.toNioPath())
      }
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
    if (bazelRunner == null) {
      System.err.println("Bazel Query directory not set or other error occurred")
      resultField.text = "Bazel Query directory not set or other error occurred"
      return
    }

    /*val queryQuotes = if (editorTextField.text.contains("\"")) "'" else "\""
    val resultTextBuilder = StringBuilder("bazel query $queryQuotes${editorTextField.text}$queryQuotes")

    defaultFlags
      .filter { it.isSelected() }
      .forEach { resultTextBuilder.append(" --").append(it.flag) }

    resultTextBuilder.append(" ").append(flagTextField.text)
    resultField.text = resultTextBuilder.toString()*/

    val label = RelativeLabel(Package(listOf(editorTextField.text)), AmbiguousEmptyTarget)
    val commandToRun = bazelRunner!!.buildBazelCommand {
      query { targets.add(label) }
    }
    commandToRun.options.clear()

    for (flag in defaultFlags) {
      if (flag.isSelected()) {
        commandToRun.options.add("--" + flag.flag)
      }
    }
    commandToRun.options.add(flagTextField.text)

    val commandResults = runBlocking {
      bazelRunner!!
        .runBazelCommand(commandToRun, serverPidFuture = null)
        .waitAndGetResult()
    }

    if (commandResults.isSuccess) {
      resultField.text = commandResults.stdout
    } else {
      resultField.text = "Command execution failed:\n" + commandResults.stderr
    }
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
