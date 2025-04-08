package org.jetbrains.bazel.ui.dialogs.queryTab

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.HyperlinkInfoBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
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
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import org.jdesktop.swingx.VerticalLayout
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.coroutines.CoroutineService
import org.jetbrains.bazel.languages.bazelquery.BazelqueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelqueryLanguage
import org.jetbrains.bazel.languages.bazelquery.options.BazelqueryCommonOptions
import org.jetbrains.bazel.ui.console.BazelBuildTargetConsoleFilter
import org.jetbrains.bazel.utils.BazelWorkingDirectoryManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.OutputStreamWriter
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

private class QueryFlagField(
  val flag: String,
  checked: Boolean = false,
  val values: List<String> = emptyList(),
) {
  val checkBox = JCheckBox(flag, checked).apply {
    addActionListener {
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
  private val queryEvaluator = QueryEvaluator(project.baseDir)

  // Graph Window Manager
  private val graphWindowManager = GraphWindowManager()


  // UI elements
  private val editorTextField = LanguageTextField(BazelqueryLanguage, project, "")
  private val directoryField = JBTextField().apply { isEditable = false }
  private val flagTextField = LanguageTextField(BazelqueryFlagsLanguage, project, "")
  private val buttonsPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    maximumSize = Dimension(Int.MAX_VALUE, 40)
  }
  private val flagsPanel = JPanel(VerticalLayout()).apply {
    defaultFlags.forEach {
      it.addToPanel(this)
    }
    add(flagTextField)
  }
  private val bazelFilter = BazelBuildTargetConsoleFilter(project)
  private val resultField: ConsoleView = ConsoleViewImpl(project, false).apply {

  }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    chooseDirectory(project.baseDir)
    initializeUI()
  }

  // Clickable targets in output
  private fun addLinksToResult(
    text: String,
    hyperlinkInfoList: MutableList<Pair<IntRange, HyperlinkInfo>>
  ) {
    val filterResult = bazelFilter.applyFilter(text, text.length)

    filterResult?.resultItems?.forEachIndexed { index, item ->
      hyperlinkInfoList.add(
        Pair(
          IntRange(item.highlightStartOffset, item.highlightEndOffset),
          item.hyperlinkInfo as HyperlinkInfo
        )
      )
    }
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
    fun createDirectorySelectionPanel() = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
      val directoryButton = JButton("Select").apply { addActionListener { chooseDirectory() } }

      add(JLabel("Selected Directory: "))
      add(directoryField)
      add(directoryButton)

      maximumSize = Dimension(Int.MAX_VALUE, 40)
    }

    fun createQueryPanel() = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
      add(editorTextField)

      maximumSize = Dimension(Int.MAX_VALUE, 40)
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
      add(resultField.component, BorderLayout.CENTER)
    }

    add(createDirectorySelectionPanel())
    add(createQueryPanel())
    setButtonsPanelToEvaluate()
    add(buttonsPanel)
    add(JBScrollPane(
      JPanel(VerticalLayout()).apply {
        add(createFlagsPanel())
        add(createResultPanel())
      }
    ))
  }

  private fun setButtonsPanelToEvaluate() {
    SwingUtilities.invokeLater {
      with(buttonsPanel) {
        removeAll()
        add(Box.createHorizontalGlue())
        val evaluateButton = JButton("Evaluate").apply {
          addActionListener { evaluate() }
        }
        add(evaluateButton)
        updateUI()
      }
    }
  }

  private fun setButtonsPanelToCancel() {
    SwingUtilities.invokeLater {
      with(buttonsPanel) {
        removeAll()
        add(Box.createHorizontalGlue())
        val evaluateButton = JButton("Cancel").apply {
          addActionListener { cancelEvaluate() }
        }
        add(evaluateButton)
        updateUI()
      }
    }
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
    val flagsToRun = mutableListOf<BazelFlag>()
    for (flag in defaultFlags) {
      if (flag.isSelected) {
        var option = flag.flag
        if (flag.values.isNotEmpty()) {
          val selectedValue = flag.valuesGroup.elements.toList().find { it.isSelected }?.text
          option += "=$selectedValue"
        }
        flagsToRun.add(BazelFlag(option))
      }
    }
    flagsToRun.addAll(BazelFlag.fromTextField(flagTextField.text))

    showInConsole("Bazel Query in progress...")

    queryEvaluator.orderEvaluation(editorTextField.text, flagsToRun)
    SwingUtilities.invokeLater { setButtonsPanelToCancel() }
    var commandResults: BazelProcessResult? = null
    CoroutineService.getInstance(project).start {
      commandResults = queryEvaluator.waitAndGetResults()
    }.invokeOnCompletion {
      SwingUtilities.invokeLater {
        if (commandResults == null) {
          showInConsole("Query cancelled")
        } else {
          if (commandResults!!.isSuccess) {
            val res = commandResults!!.stdout
            if (res.isEmpty()) {
              showInConsole("Nothing found")
            } else {
                val hyperlinkInfoList = mutableListOf<Pair<IntRange, HyperlinkInfo>>()
                addLinksToResult(res, hyperlinkInfoList)
                showInConsole(res, hyperlinkInfoList)
              if (res.startsWith("digraph")) {
                val imageIcon = convertDotToImageIcon(res)
                if (imageIcon != null) {
                  graphWindowManager.openImageInNewWindow(imageIcon)
                } else {
                  //System.err.println("Failed to generate graph visualization")
                  NotificationGroupManager.getInstance()
                    .getNotificationGroup("Bazel")
                    .createNotification("Failed to generate graph visualization", NotificationType.ERROR)
                    .notify(project)
                }
              }
            }
          } else {
            showInConsole("Command execution failed:\n" + commandResults!!.stderr)
          }
        }

        setButtonsPanelToEvaluate()
      }
    }
  }

  private fun cancelEvaluate() {
    queryEvaluator.cancelEvaluation()
  }


  private fun showInConsole(
    text: String,
    hyperlinkInfoList: List<Pair<IntRange, HyperlinkInfo?>> = emptyList()
  ) {
    resultField.clear()

    var lastIndex = 0
    for ((range, hyperlinkInfo) in hyperlinkInfoList) {
      if (range.first > lastIndex) {
        val normalText = text.substring(lastIndex, range.first)
        resultField.print(normalText, ConsoleViewContentType.NORMAL_OUTPUT)
      }
      val hyperlinkText = text.substring(range.first, range.last)
      resultField.printHyperlink(hyperlinkText, object : HyperlinkInfoBase() {
        override fun navigate(project: Project, hyperlinkLocationPoint: RelativePoint?) {
          hyperlinkInfo?.navigate(project)
        }
      })

      lastIndex = range.last
    }

    if (lastIndex < text.length) {
      val normalText = text.substring(lastIndex)
      resultField.print(normalText, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    updateUI()
  }
}
