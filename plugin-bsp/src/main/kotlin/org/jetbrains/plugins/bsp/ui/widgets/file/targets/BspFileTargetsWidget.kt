package org.jetbrains.plugins.bsp.ui.widgets.file.targets

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.extension.points.targetActionProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.actions.target.BuildTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.fillWithEligibleActions
import javax.swing.Icon

private const val ID = "org.jetbrains.bsp.BspFileTargetsWidget"

public class BspFileTargetsWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {
  init {
    project.temporaryTargetUtils.registerListener { update() }
  }

  override fun ID(): String = ID

  override fun getWidgetState(file: VirtualFile?): WidgetState =
    if (file == null) inactiveWidgetState(project.assets.icon)
    else activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file, project.assets.icon)

  private fun activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file: VirtualFile, icon: Icon): WidgetState {
    val targets = project.temporaryTargetUtils.getTargetsForFile(file, project)
    return if (targets.isEmpty()) inactiveWidgetState(icon)
    else activeWidgetState(targets.firstOrNull(), icon)
  }

  private fun inactiveWidgetState(icon: Icon): WidgetState {
    val state = WidgetState(BspPluginBundle.message("widget.tooltip.text.inactive"), "", false)
    state.icon = icon

    return state
  }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-988
  private fun activeWidgetState(loadedTarget: BuildTargetIdentifier?, icon: Icon): WidgetState {
    val text = loadedTarget?.uri ?: ""
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
    val targetIds = project.temporaryTargetUtils.getTargetsForFile(file, project)
    val targets = targetIds.mapNotNull { project.temporaryTargetUtils.getBuildTargetInfoForId(it) }
    val groups = targets.map { it.calculatePopupGroup() }

    return DefaultActionGroup(groups)
  }

  private fun BuildTargetInfo.calculatePopupGroup(): ActionGroup =
    DefaultActionGroup(id, true).also {
      if (capabilities.canCompile) {
        it.add(BuildTargetAction(id))
      }
      it.fillWithEligibleActions(this, false)
      it.addSeparator()
      it.addAll(project.targetActionProvider?.getTargetActions(component, project, this).orEmpty())
    }

  override fun createInstance(project: Project): StatusBarWidget =
    BspFileTargetsWidget(project)
}

public class BspFileTargetsWidgetFactory : StatusBarWidgetFactory {
  override fun getId(): String = ID

  override fun getDisplayName(): String =
    BspPluginBundle.message("widget.factory.display.name")

  override fun isAvailable(project: Project): Boolean =
    project.isBspProject

  override fun createWidget(project: Project): StatusBarWidget =
    BspFileTargetsWidget(project)

  override fun disposeWidget(widget: StatusBarWidget) {
    Disposer.dispose(widget)
  }

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean =
    true
}

internal fun Project.updateBspFileTargetsWidget() {
  val statusBarWidgetsManager = service<StatusBarWidgetsManager>()
  statusBarWidgetsManager.updateWidget(BspFileTargetsWidgetFactory())
}
