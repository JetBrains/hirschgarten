package org.jetbrains.bazel.ui.queryTab

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.HyperlinkInfoBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteIntentReadAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
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
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage
import org.jetbrains.bazel.languages.bazelquery.options.BazelQueryCommonOptions
import org.jetbrains.bazel.ui.console.BazelBuildTargetConsoleFilter
import org.jetbrains.bazel.utils.BazelWorkingDirectoryManager
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
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
  val checkBox =
    JCheckBox(flag, checked).apply {
      addActionListener {
        valuesButtons.forEach { it.isVisible = isSelected }
      }
    }
  val valuesButtons: List<JRadioButton> =
    values.map {
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

class BazelQueryTab(private val project: Project) : JPanel() {
  private val defaultFlags =
    BazelQueryCommonOptions.getAll().map { option ->
      QueryFlagField(
        flag = option.name,
        values = option.values,
      )
    }

  // Bazel Runner
  private val queryEvaluator = QueryEvaluator(project.baseDir)

  // UI elements
  private val editorTextField = LanguageTextField(BazelQueryLanguage, project, "")
  private val directoryField = JBTextField().apply { isEditable = false }
  private val flagTextField = LanguageTextField(BazelQueryFlagsLanguage, project, "")
  private val buttonsPanel =
    JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
      maximumSize = Dimension(Int.MAX_VALUE, 40)
    }
  private val flagsPanel =
    JPanel(VerticalLayout()).apply {
      defaultFlags.forEach {
        it.addToPanel(this)
      }
      add(flagTextField)
    }
  private val bazelFilter = BazelBuildTargetConsoleFilter(project)
  private val resultField: ConsoleView =
    ConsoleViewImpl(project, false).apply {
    }

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    chooseDirectory(project.baseDir)
    initializeUI()
  }

  // Clickable targets in output
  private fun addLinksToResult(text: String, hyperlinkInfoList: MutableList<Pair<IntRange, HyperlinkInfo>>) {
    val filterResult = WriteIntentReadAction.compute<Filter.Result> { bazelFilter.applyFilter(text, text.length) }

    filterResult?.resultItems?.forEachIndexed { index, item ->
      hyperlinkInfoList.add(
        Pair(
          IntRange(item.highlightStartOffset, item.highlightEndOffset),
          item.hyperlinkInfo as HyperlinkInfo,
        ),
      )
    }
  }

  // Graph visualization
  private fun showGraph(dotContent: String) {
    val dotUtilName = if (SystemInfo.isUnix) "dot" else "dot.exe"
    val dotFullPath =
      PathEnvironmentVariableUtil.findInPath(dotUtilName)
        ?: run {
          val notificationText =
            if (SystemInfo.isUnix) {
              "Probably graphviz is missing. You could install graphviz using `apt install graphviz` or `brew install graphviz`"
            } else {
              "Probably graphviz is missing. You can download it from https://graphviz.org/download/."
            }

          NotificationGroupManager
            .getInstance()
            .getNotificationGroup("Bazel")
            .createNotification(notificationText, NotificationType.ERROR)
            .notify(project)
          return
        }

    try {
      val svgFile = FileUtil.createTempFile("bazelqueryGraph", ".svg", true)
      val dotFile = FileUtil.createTempFile("tempDot", ".dot", true)
      FileUtil.writeToFile(dotFile, dotContent)

      val commandLine =
        GeneralCommandLine()
          .withExePath(dotFullPath.absolutePath)
          .withParameters("-Tsvg", "-o${svgFile.absolutePath}", dotFile.absolutePath)

      val processHandler = OSProcessHandler(commandLine)
      processHandler.startNotify()
      val success = processHandler.waitFor()

      if (!success) {
        throw Exception()
      }

      ApplicationManager.getApplication().invokeLater {
        BrowserUtil.browse(svgFile)
      }
    } catch (_: Exception) {
      NotificationGroupManager
        .getInstance()
        .getNotificationGroup("Bazel")
        .createNotification("Failed to generate graph visualization", NotificationType.ERROR)
        .notify(project)
    }
  }

  private fun initializeUI() {
    fun createDirectorySelectionPanel() =
      JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        val directoryButton = JButton("Select").apply { addActionListener { chooseDirectory() } }

        add(JLabel("Selected Directory: "))
        add(directoryField)
        add(directoryButton)

        maximumSize = Dimension(Int.MAX_VALUE, 40)
      }

    fun createQueryPanel() =
      JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(editorTextField)

        maximumSize = Dimension(Int.MAX_VALUE, 40)
      }

    fun createFlagsPanel() =
      CollapsiblePanel(
        JBScrollPane(flagsPanel).apply {
          verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        },
        true,
        true,
        AllIcons.General.ChevronUp,
        AllIcons.General.ChevronDown,
        "Flags",
      )

    fun createResultPanel() =
      JPanel(BorderLayout()).apply {
        add(resultField.component, BorderLayout.CENTER)
      }

    add(createDirectorySelectionPanel())
    add(createQueryPanel())
    setButtonsPanelToEvaluate()
    add(buttonsPanel)
    add(
      JBScrollPane(
        JPanel(VerticalLayout()).apply {
          add(createFlagsPanel())
          add(createResultPanel())
        },
      ),
    )
  }

  private fun setButtonsPanelToEvaluate() {
    SwingUtilities.invokeLater {
      with(buttonsPanel) {
        removeAll()
        add(Box.createHorizontalGlue())
        val evaluateButton =
          JButton("Evaluate").apply {
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
        val evaluateButton =
          JButton("Cancel").apply {
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
    val chosenDir =
      if (dirFile != null) {
        if (!dirFile.isDirectory) throw IllegalArgumentException("$dirFile is not a directory")
        dirFile
      } else {
        val descriptor =
          FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
            title = "Select Directory"
            description = "Choose a directory within the project."
          }
        FileChooser.chooseFile(descriptor, project, null)
      }

    if (chosenDir != null) {
      val relativePath =
        VfsUtilCore.getRelativePath(
          chosenDir,
          project.baseDir ?: chosenDir,
          '/',
        )

      if (relativePath == null) {
        if (dirFile != null) throw IllegalArgumentException("$dirFile is not in project")

        NotificationGroupManager
          .getInstance()
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
          val selectedValue =
            flag.valuesGroup.elements
              .toList()
              .find { it.isSelected }
              ?.text
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
    BazelCoroutineService
      .getInstance(project)
      .start {
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
                  showGraph(res)
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

  private fun showInConsole(text: String, hyperlinkInfoList: List<Pair<IntRange, HyperlinkInfo?>> = emptyList()) {
    resultField.clear()

    var lastIndex = 0
    for ((range, hyperlinkInfo) in hyperlinkInfoList) {
      if (range.first > lastIndex) {
        val normalText = text.substring(lastIndex, range.first)
        resultField.print(normalText, ConsoleViewContentType.NORMAL_OUTPUT)
      }
      val hyperlinkText = text.substring(range.first, range.last)
      resultField.printHyperlink(
        hyperlinkText,
        object : HyperlinkInfoBase() {
          override fun navigate(project: Project, hyperlinkLocationPoint: RelativePoint?) {
            hyperlinkInfo?.navigate(project)
          }
        },
      )

      lastIndex = range.last
    }

    if (lastIndex < text.length) {
      val normalText = text.substring(lastIndex)
      resultField.print(normalText, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    updateUI()
  }
}
