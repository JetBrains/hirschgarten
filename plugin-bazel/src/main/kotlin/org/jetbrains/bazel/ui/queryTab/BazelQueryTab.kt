package org.jetbrains.bazel.ui.queryTab

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.HyperlinkInfoBase
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteIntentReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.JBSplitter
import com.intellij.ui.LanguageTextField
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.jdesktop.swingx.VerticalLayout
import org.jetbrains.bazel.bazelrunner.BazelProcessResult
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.languages.bazelquery.BazelQueryFlagsLanguage
import org.jetbrains.bazel.languages.bazelquery.BazelQueryLanguage
import org.jetbrains.bazel.languages.bazelquery.options.BazelQueryCommonOptions
import org.jetbrains.bazel.ui.console.BazelBuildTargetConsoleFilter
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.AbstractButton
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.KeyStroke
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
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
      addShiftEnterAction()
    }
  val valuesButtons: List<JRadioButton> =
    values.map {
      JRadioButton(it).apply {
        border = JBUI.Borders.emptyLeft(25)
        addShiftEnterAction()
        isFocusable = true
        setFocusable(true)
      }
    }
  val valuesGroup: ButtonGroup = ButtonGroup()

  init {
    valuesButtons.forEach {
      this.valuesGroup.add(it)
      it.isVisible = false
    }
    valuesButtons.firstOrNull()?.isSelected = true
  }

  private fun AbstractButton.addShiftEnterAction() {
    addKeyListener(
      object : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
          if (e.keyCode == KeyEvent.VK_ENTER && e.isShiftDown) {
            doClick()
            e.consume()
          }
        }
      },
    )
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

  private class EvaluateQueryAction(private val tab: BazelQueryTab) : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
      tab.evaluate()
    }
  }

  // UI elements
  private val editorTextField =
    LanguageTextField(BazelQueryLanguage, project, "").apply {
      setPlaceholder(BazelPluginBundle.message("bazel.toolwindow.tab.query.placeholder.query"))
      registerKeyboardAction(
        {
          val editor = getEditor()
          if (editor != null) {
            val lookup = LookupManager.getActiveLookup(editor)
            if (lookup == null) {
              evaluate()
            }
          }
        },
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
        WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
      )
    }
  private val flagTextField =
    LanguageTextField(BazelQueryFlagsLanguage, project, "").apply {
      setPlaceholder(BazelPluginBundle.message("bazel.toolwindow.tab.query.placeholder.flags"))
    }
  private val buttonsPanel =
    JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
    }
  private val flagsPanel =
    JPanel(VerticalLayout()).apply {
      add(
        JLabel(BazelPluginBundle.message("label.bazel.query.flag.panel.tittle")).apply {
          horizontalAlignment = SwingConstants.CENTER
          font = font.deriveFont(Font.BOLD)
          border = JBUI.Borders.empty(5)
        },
      )
      defaultFlags.forEach {
        it.addToPanel(this)
      }
      add(flagTextField)
      val action = EvaluateQueryAction(this@BazelQueryTab)
      action.registerCustomShortcutSet(
        CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)),
        this,
      )
    }
  private val resultField: ConsoleView = ConsoleViewImpl(project, false)

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    initializeUI()
  }

  // Clickable targets in output
  private val bazelFilter = BazelBuildTargetConsoleFilter(project)

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
              BazelPluginBundle.message("notification.bazel.query.graphviz.missing.unix")
            } else {
              BazelPluginBundle.message("notification.bazel.query.graphviz.missing.nonunix")
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
        .createNotification(BazelPluginBundle.message("notification.bazel.query.graph.visualization.failed"), NotificationType.ERROR)
        .notify(project)
    }
  }

  // UI
  private fun initializeUI() {
    fun createQueryPanel() =
      JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(editorTextField)
        add(buttonsPanel)

        maximumSize = Dimension(Int.MAX_VALUE, 40)
      }

    fun createFlagsPanel() =
      ScrollToFocusedFlagPanel(flagsPanel).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
      }

    fun createResultPanel() =
      JPanel(BorderLayout()).apply {
        add(resultField.component, BorderLayout.CENTER)
      }

    setButtonsPanelToEvaluate()
    add(createQueryPanel())

    val splitter =
      JBSplitter(false, 0.25f, 0.2f, 0.8f).apply {
        firstComponent = createFlagsPanel()
        secondComponent =
          JBScrollPane(
            JPanel().apply {
              layout = BoxLayout(this, BoxLayout.Y_AXIS)
              add(createResultPanel())
            },
          )

        setHonorComponentsMinimumSize(true)
      }

    add(splitter)
  }

  private fun setButtonsPanelToEvaluate() {
    SwingUtilities.invokeLater {
      with(buttonsPanel) {
        removeAll()
        val evaluateButton =
          JButton(BazelPluginBundle.message("button.bazel.query.evaluate")).apply {
            addActionListener { evaluate() }
            addKeyListener(
              object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                  if (e.keyCode == KeyEvent.VK_ENTER) {
                    doClick()
                    e.consume()
                  }
                }
              },
            )
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
        val evaluateButton =
          JButton(BazelPluginBundle.message("button.bazel.query.cancel")).apply {
            addActionListener { cancelEvaluate() }
            addKeyListener(
              object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                  if (e.keyCode == KeyEvent.VK_ENTER) {
                    doClick()
                    e.consume()
                  }
                }
              },
            )
          }
        add(evaluateButton)
        updateUI()
      }
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

    showInConsole(BazelPluginBundle.message("bazel.toolwindow.tab.query.output.in.progress"))

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
            showInConsole(BazelPluginBundle.message("bazel.toolwindow.tab.query.output.cancelled"))
          } else {
            if (commandResults!!.isSuccess) {
              val res = commandResults!!.stdout
              if (res.isEmpty()) {
                showInConsole(BazelPluginBundle.message("bazel.toolwindow.tab.query.output.nothing"))
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
