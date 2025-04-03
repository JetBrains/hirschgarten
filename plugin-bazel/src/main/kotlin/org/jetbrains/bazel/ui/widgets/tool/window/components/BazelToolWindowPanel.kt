package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeIntentReadAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBTabbedPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.services.invalidTargets
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.dialogs.queryTab.BazelQueryDialogWindow
import org.jetbrains.bazel.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.bazel.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.bazel.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.bazel.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import java.nio.file.Path
import javax.swing.SwingConstants

class BazelToolWindowPanel(val project: Project) : SimpleToolWindowPanel(true, true) {
  private val targetFilter = TargetFilter { rerenderComponents() }
  private val searchBarPanel = SearchBarPanel()
  private var loadedTargetsPanel: BspPanelComponent
  private val bazelQueryDialogWindow = BazelQueryDialogWindow(project)


  init {
    val actionManager = ActionManager.getInstance()
    val targetUtils = project.targetUtils

    loadedTargetsPanel = createLoadedTargetsPanel(project, targetUtils)

    val defaultActions = actionManager.getAction("Bazel.ActionsToolbar")
    val actionGroup =
      DefaultActionGroup().apply {
        addAll(defaultActions)
        addSeparator()
        add(FilterActionGroup(targetFilter))
        addSeparator()
        add(BazelToolWindowSettingsAction(BazelPluginBundle.message("project.settings.display.name")))
        addSeparator()
        add(BazelToolWindowConfigFileOpenAction())
      }

    val actionToolbar =
      actionManager.createActionToolbar("Bazel Toolbar", actionGroup, true).apply {
        targetComponent = this@BazelToolWindowPanel.component
        orientation = SwingConstants.HORIZONTAL
      }

    this.toolbar = actionToolbar.component

    val tabbedPane = JBTabbedPane().apply {
      addTab("Loaded Targets", loadedTargetsPanel.withScrollAndSearch())
      addTab("Bazel Query", bazelQueryDialogWindow)
    }
    setContent(tabbedPane)

    targetUtils.registerSyncListener { targetListChanged ->
      if (targetListChanged) {
        ApplicationManager.getApplication().invokeLater {
          rerenderComponents()
        }
      }
    }
  }

  private fun createLoadedTargetsPanel(project: Project, targetUtils: TargetUtils): BspPanelComponent =
    BspPanelComponent(
      project = project,
      targetIcon = BazelPluginIcons.bazel,
      invalidTargetIcon = BazelPluginIcons.bazelError,
      toolName = BazelPluginConstants.BAZEL_DISPLAY_NAME,
      targets = targetUtils.allTargets().mapNotNull { targetUtils.getBuildTargetForLabel(it) },
      invalidTargets = project.invalidTargets,
      searchBarPanel = searchBarPanel,
    ).apply {
      registerPopupHandler { LoadedTargetsMouseListener(it, project) }
    }

  private fun rerenderComponents() {
    val targetUtils = project.targetUtils
    searchBarPanel.clearAllListeners()
    loadedTargetsPanel =
      loadedTargetsPanel.createNewWithTargets(targetFilter.getMatchingLoadedTargets(targetUtils), project.invalidTargets)
    setContent(loadedTargetsPanel.withScrollAndSearch())
  }
}

private class BazelToolWindowSettingsAction(private val settingsDisplayName: String) :
  SuspendableAction(
    { BazelPluginBundle.message("widget.settings.popup.message", settingsDisplayName) },
    AllIcons.General.Settings,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val showSettingsUtil = ShowSettingsUtil.getInstance()
    withContext(Dispatchers.EDT) {
      writeIntentReadAction {
        showSettingsUtil.showSettingsDialog(project, settingsDisplayName)
      }
    }
  }
}

private class BazelToolWindowConfigFileOpenAction :
  SuspendableAction(
    { BazelPluginBundle.message("widget.config.file.popup.message", BazelPluginBundle.message("tool.window.generic.config.file")) },
    AllIcons.FileTypes.Config,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val configFile = project.bazelProjectSettings.projectViewPath
    e.presentation.isEnabled = configFile != null
    withContext(Dispatchers.EDT) {
      ProjectView.getInstance(project).refresh()
      configFile?.getPsiFile(project)?.navigate(true)
    }
  }
}

private fun Path.getPsiFile(project: Project): PsiFile? {
  val virtualFileManager = VirtualFileManager.getInstance()
  val virtualFile =
    virtualFileManager.findFileByNioPath(this) ?: return null
  return PsiManager.getInstance(project).findFile(virtualFile)
}
