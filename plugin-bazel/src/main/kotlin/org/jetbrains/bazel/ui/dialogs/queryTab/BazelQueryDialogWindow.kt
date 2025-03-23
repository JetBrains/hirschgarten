package org.jetbrains.bazel.ui.dialogs.queryTab

import com.intellij.execution.filters.HyperlinkInfo
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
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.coroutines.CoroutineService
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.ui.console.BazelBuildTargetConsoleFilter
import org.jetbrains.bazel.utils.BazelWorkingDirectoryManager
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities
import javax.swing.event.HyperlinkEvent
import java.io.OutputStreamWriter
import javax.swing.ImageIcon

import org.jetbrains.bazel.languages.bazelquery.options.BazelqueryCommonOptions
import javax.swing.ButtonGroup
import javax.swing.JRadioButton
import javax.swing.border.EmptyBorder

private class QueryFlagField(
  val flag: String,
  checked: Boolean = false,
  val values: List<String> = emptyList(),
) {
  val checkBox = JCheckBox(flag, checked).apply {
    addActionListener {
      updateFlagCount(isSelected)
      valuesButtons.forEach { it.isVisible = isSelected }
    }
  }
  val valuesButtons: List<JRadioButton> = values.map {
    JRadioButton(it).apply { border = JBUI.Borders.emptyLeft(25) }
  }
  val valuesGroup: ButtonGroup = ButtonGroup()
  init {
    valuesButtons.forEach {
      valuesGroup.add(it)
      it.isVisible = false
    }
    valuesButtons.firstOrNull()?.isSelected = true
  }

  companion object {
    var selectedFlagCount = 0

    private fun updateFlagCount(isSelected: Boolean) {
      selectedFlagCount += if (isSelected) 1 else -1
    }
  }

  fun addToPanel(panel: JPanel) {
    panel.add(checkBox)
    valuesButtons.forEach {
      panel.add(it)
    }
  }

  val isSelected get() = checkBox.isSelected
}

// TODO: Rename to something else than window and move to correct directory
class BazelQueryDialogWindow(private val project: Project) : JPanel() {
  private val defaultFlags = BazelqueryCommonOptions().getAll().map { option ->
    QueryFlagField(
      flag = option.name,
      values = option.values
    )
  }

  // Bazel Runner
  private val queryEvaluator = QueryEvaluator()

  // Graph Window Manager
  private val graphWindowManager = GraphWindowManager()


  // UI elements
  private val editorTextField = LanguageTextField(BazelqueryLanguage, project, "")
  private val directoryField = JBTextField().apply { isEditable = false }
  private val flagTextField = LanguageTextField(BazelqueryFlagsLanguage, project, "")
  private val flagsPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    defaultFlags.forEach {
      it.addToPanel(this)
    }
    add(flagTextField)
  }
  private val bazelFilter = BazelBuildTargetConsoleFilter(project)
  private val hyperlinkInfoMap = mutableMapOf<String, HyperlinkInfo>()
  private val resultField = JEditorPane().apply {
    contentType = "text/html"
    isEditable = false
    addHyperlinkListener { event ->
      if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
        val key = event.description
        val hyperlinkInfo = hyperlinkInfoMap[key]
        hyperlinkInfo?.navigate(project)
      }
    }
  }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    chooseDirectory(project.baseDir)
    initializeUI()
  }

  // Clickable targets in output
  private fun addLinksToResult(text: String): String {
    val filterResult = bazelFilter.applyFilter(text, text.length)
    var processedText = text

    filterResult?.resultItems?.forEachIndexed { index, item ->
      val linkText = text.substring(item.highlightStartOffset, item.highlightEndOffset)

      val hyperlinkKey = index.toString()
      hyperlinkInfoMap[hyperlinkKey] = item.hyperlinkInfo as HyperlinkInfo

      val hyperlink = "<a href='$hyperlinkKey'>$linkText</a>"
      processedText = processedText.replace(linkText, hyperlink)
    }
    return processedText
  }

  // Graph visualization
  private fun convertDotToImageIcon(dotContent: String): ImageIcon? {
    return try {
      val process = ProcessBuilder("dot", "-Tpng")
        .redirectErrorStream(true)
        .start()

      OutputStreamWriter(process.outputStream).use { writer ->
        writer.write(dotContent)
        writer.flush()
      }

      val pngBytes = process.inputStream.readBytes()
      val exitCode = process.waitFor()
      if (exitCode == 0)
        ImageIcon(pngBytes)
      else
        null
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
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
      JBScrollPane(flagsPanel).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
      },
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
        var option = flag.flag
        if (flag.values.isNotEmpty()) {
          val selectedValue = flag.valuesGroup.elements.toList().find { it.isSelected }?.text
          option += "=$selectedValue"
        }
        flagsToRun.add(option)
      }
    }
    resultField.text = "Bazel Query in progress..."

    var commandResults: BazelProcessResult? = null
    CoroutineService.getInstance(project).start {
      commandResults = queryEvaluator.evaluate(editorTextField.text, flagsToRun, flagTextField.text)
    }.invokeOnCompletion {
      SwingUtilities.invokeLater {
        if (commandResults!!.isSuccess) {
          val res = commandResults.stdout
          if (res.isEmpty()) {
            resultField.text = "Nothing found"
          } else {
            resultField.text = "<html><pre>${addLinksToResult(res.replace("//", "&#47;&#47;"))}</pre></html>"
            if (res.startsWith("digraph")) {
              val imageIcon = convertDotToImageIcon(res)
              if (imageIcon != null) {
                graphWindowManager.openImageInNewWindow(imageIcon)
              } else {
                System.err.println("Failed to generate graph visualization")
              }
            }
          }
        } else {
          resultField.text = "Command execution failed:\n" + commandResults.stderr
        }
        updateUI()
      }
    }
  }

  private fun clear() {
    editorTextField.text = ""
    resultField.text = ""
  }
}
