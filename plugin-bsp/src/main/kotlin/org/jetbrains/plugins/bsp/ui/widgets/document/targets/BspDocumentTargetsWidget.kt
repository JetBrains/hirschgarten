package org.jetbrains.plugins.bsp.ui.widgets.document.targets

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup
import org.jetbrains.magicmetamodel.DocumentTargetsDetails
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.BspProjectPropertiesService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

private const val ID = "BspDocumentTargetsWidget"

private class LoadTargetAction(
  private val target: BuildTargetIdentifier,
  private val updateWidget: () -> Unit
) : AnAction(target.uri) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doAction(project)
    } else {
      log.warn("LoadTargetAction cannot be performed! Project not available.")
    }
  }

  private fun doAction(project: Project) {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    val diff = magicMetaModel.loadTarget(target)
    runWriteAction { diff?.applyOnWorkspaceModel() }

    updateWidget()
  }

  private companion object {
    private val log = logger<LoadTargetAction>()
  }
}

public class BspDocumentTargetsWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {

  private val magicMetaModelService = MagicMetaModelService.getInstance(project)

  init {
    magicMetaModelService.value.registerTargetLoadListener { update(); }
  }

  override fun ID(): String = ID

  override fun getWidgetState(file: VirtualFile?): WidgetState =
    if (file == null) inactiveWidgetState() else activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file)

  private fun activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file: VirtualFile): WidgetState {
    val documentDetails = getDocumentDetails(file)

    return when {
      documentDetails.loadedTargetId == null && documentDetails.notLoadedTargetsIds.isEmpty() -> inactiveWidgetState()
      else -> activeWidgetState(documentDetails.loadedTargetId)
    }
  }

  private fun inactiveWidgetState(): WidgetState {
    val state = WidgetState(BspDocumentTargetsWidgetBundle.message("widget.tooltip.text.inactive"), "", false)
    state.icon = BspPluginIcons.bsp

    return state
  }

  private fun activeWidgetState(loadedTarget: BuildTargetIdentifier?): WidgetState {
    val text = loadedTarget?.uri ?: ""
    val state = WidgetState(BspDocumentTargetsWidgetBundle.message("widget.tooltip.text.active"), text, true)
    state.icon = BspPluginIcons.bsp

    return state
  }

  override fun createPopup(context: DataContext): ListPopup {
    val file = CommonDataKeys.VIRTUAL_FILE.getData(context)!!
    val group = calculatePopupGroup(file)
    val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
    val title = BspDocumentTargetsWidgetBundle.message("widget.title")

    return JBPopupFactory.getInstance().createActionGroupPopup(title, group, context, mnemonics, true)
  }

  private fun calculatePopupGroup(file: VirtualFile): ActionGroup {
    val documentDetails = getDocumentDetails(file)

    val group = DefaultActionGroup()
    updateActionGroupWithCurrentlyLoadedTarget(group, documentDetails.loadedTargetId)
    updateActionGroupWithAvailableTargetsSection(group, documentDetails.notLoadedTargetsIds)

    return group
  }

  private fun updateActionGroupWithCurrentlyLoadedTarget(
    group: DefaultActionGroup,
    loadedTarget: BuildTargetIdentifier?
  ) {
    group.addSeparator(BspDocumentTargetsWidgetBundle.message("widget.loaded.target.separator.title"))

    if (loadedTarget != null) {
      group.addAction(LoadTargetAction(loadedTarget) { update() })
    }
  }

  private fun updateActionGroupWithAvailableTargetsSection(
    group: DefaultActionGroup,
    notLoadedTargetsIds: List<BuildTargetIdentifier>
  ) {
    group.addSeparator(BspDocumentTargetsWidgetBundle.message("widget.available.targets.to.load"))

    val actions = notLoadedTargetsIds.map { LoadTargetAction(it) { update() } }
    group.addAll(actions)
  }

  private fun getDocumentDetails(file: VirtualFile): DocumentTargetsDetails =
    magicMetaModelService.value.getTargetsDetailsForDocument(TextDocumentIdentifier(file.url))

  override fun createInstance(project: Project): StatusBarWidget =
    BspDocumentTargetsWidget(project)
}

public class BspDocumentTargetsWidgetFactory : StatusBarWidgetFactory {

  override fun getId(): String = ID

  override fun getDisplayName(): String =
    BspDocumentTargetsWidgetBundle.message("widget.factory.display.name")

  override fun isAvailable(project: Project): Boolean =
    BspProjectPropertiesService.getInstance(project).value.isBspProject

  override fun createWidget(project: Project): StatusBarWidget =
    BspDocumentTargetsWidget(project)

  override fun disposeWidget(widget: StatusBarWidget) {
    Disposer.dispose(widget)
  }

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean =
    true
}
