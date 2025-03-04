package org.jetbrains.bazel.ui.widgets.fileTargets

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.config.isBspProject
import org.jetbrains.bazel.debug.actions.StarlarkDebugAction
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.runnerAction.BuildTargetAction
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.BazelBspJumpToBuildFileAction
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bazel.ui.widgets.tool.window.utils.fillWithEligibleActions
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import javax.swing.Icon

/**
 * Make sure this id matches the one of the extension registration <statusBarWidgetFactory/> in the plugin.xml file
 */
private const val WIDGET_ID = "BspFileTargetsWidget"

class BspFileTargetsWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {
  init {
    project.targetUtils.registerSyncListener {
      ApplicationManager.getApplication().invokeLater {
        update()
      }
    }
  }

  override fun ID(): String = WIDGET_ID

  override fun getWidgetState(file: VirtualFile?): WidgetState =
    if (file == null) {
      inactiveWidgetState(BazelPluginIcons.bazel)
    } else {
      activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file, BazelPluginIcons.bazel)
    }

  private fun activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file: VirtualFile, icon: Icon): WidgetState {
    val targets = project.targetUtils.getTargetsForFile(file)
    return if (targets.isEmpty()) {
      inactiveWidgetState(icon)
    } else {
      activeWidgetState(targets.firstOrNull(), icon)
    }
  }

  private fun inactiveWidgetState(icon: Icon): WidgetState {
    val state = WidgetState(BspPluginBundle.message("widget.tooltip.text.inactive"), "", false)
    state.icon = icon

    return state
  }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-988
  private fun activeWidgetState(loadedTarget: Label?, icon: Icon): WidgetState {
    val text = loadedTarget?.toString() ?: ""
    val state = WidgetState(BspPluginBundle.message("widget.tooltip.text.active"), text, true)
    state.icon = icon

    return state
  }

  override fun createPopup(context: DataContext): ListPopup {
    val file = CommonDataKeys.VIRTUAL_FILE.getData(context)!!
    val group = calculatePopupGroup(file)
    val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
    val title = BspPluginBundle.message("widget.title")

    return JBPopupFactory.getInstance().createActionGroupPopup(title, group, context, mnemonics, true)
  }

  private fun calculatePopupGroup(file: VirtualFile): ActionGroup {
    val targetUtils = project.targetUtils
    val targetIds = targetUtils.getTargetsForFile(file)
    val executableTargetIds = targetUtils.getExecutableTargetsForFile(file) - targetIds.toSet()

    val targets = targetIds.getTargetInfos()
    val executableTargets = executableTargetIds.getTargetInfos()

    return DefaultActionGroup().also {
      it.addAll(targets.map { it.calculatePopupGroup() })
      if (targets.isNotEmpty() && executableTargets.isNotEmpty()) it.addSeparator()
      it.addAll(executableTargets.map { it.calculatePopupGroup() })
    }
  }

  private fun List<Label>.getTargetInfos(): List<BuildTargetInfo> = this.mapNotNull { project.targetUtils.getBuildTargetInfoForLabel(it) }

  private fun BuildTargetInfo.calculatePopupGroup(): ActionGroup =
    DefaultActionGroup(id.toShortString(), true).also {
      ResyncTargetAction.createIfEnabled(id)?.let { resyncTargetAction -> it.add(resyncTargetAction) }
      it.add(CopyTargetIdAction.FromTargetInfo(this))
      it.addSeparator()
      if (capabilities.canCompile) {
        it.add(BuildTargetAction(id))
      }
      it.fillWithEligibleActions(project, this, false)
      it.addSeparator()
      it.add(BazelBspJumpToBuildFileAction(this))
      if (StarlarkDebugAction.isApplicableTo(this)) it.add(StarlarkDebugAction(this.id))
    }

  override fun createInstance(project: Project): StatusBarWidget = BspFileTargetsWidget(project)
}

class BspFileTargetsWidgetFactory : StatusBarWidgetFactory {
  override fun getId(): String = WIDGET_ID

  override fun getDisplayName(): String = BspPluginBundle.message("widget.factory.display.name")

  override fun isAvailable(project: Project): Boolean = project.isBspProject

  override fun createWidget(project: Project): StatusBarWidget = BspFileTargetsWidget(project)

  override fun disposeWidget(widget: StatusBarWidget) {
    Disposer.dispose(widget)
  }

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

  override fun isEnabledByDefault(): Boolean = true
}

fun Project.updateBspFileTargetsWidget() {
  if (!ApplicationManager.getApplication().isHeadlessEnvironment) {
    val statusBarWidgetsManager = service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateWidget(BspFileTargetsWidgetFactory::class.java)
  }
}
