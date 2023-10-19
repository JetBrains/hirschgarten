package org.jetbrains.plugins.bsp.ui.widgets.document.targets

import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.flow.open.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.actions.LoadTargetAction
import java.net.URI
import javax.swing.Icon

private const val ID = "BspDocumentTargetsWidget"

public class BspDocumentTargetsWidget(project: Project) : EditorBasedStatusBarPopup(project, false) {
  private val magicMetaModelService = MagicMetaModelService.getInstance(project)

  init {
    magicMetaModelService.value.registerTargetLoadListener { update(); }
  }

  override fun ID(): String = ID

  override fun getWidgetState(file: VirtualFile?): WidgetState {
    val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
    return if (file == null) inactiveWidgetState(assetsExtension.icon)
    else activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file, assetsExtension.icon)
  }

  private fun activeWidgetStateIfIncludedInAnyTargetOrInactiveState(file: VirtualFile, icon: Icon): WidgetState {
    val documentDetails = getDocumentDetails(file)
    return when {
      documentDetails == null -> inactiveWidgetState(icon)
      documentDetails.loadedTargetId == null && documentDetails.notLoadedTargetsIds.isEmpty() ->
        inactiveWidgetState(icon)
      else -> activeWidgetState(documentDetails.loadedTargetId, icon)
    }
  }

  private fun inactiveWidgetState(icon: Icon): WidgetState {
    val state = WidgetState(BspPluginBundle.message("widget.tooltip.text.inactive"), "", false)
    state.icon = icon

    return state
  }

  private fun activeWidgetState(loadedTarget: BuildTargetId?, icon: Icon): WidgetState {
    val text = loadedTarget ?: ""
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
    val documentDetails = getDocumentDetails(file)

    val group = DefaultActionGroup()
    if (documentDetails != null) {
      updateActionGroupWithCurrentlyLoadedTarget(group, documentDetails.loadedTargetId)
      updateActionGroupWithAvailableTargetsSection(group, documentDetails.notLoadedTargetsIds)
    }
    return group
  }

  private fun updateActionGroupWithCurrentlyLoadedTarget(
    group: DefaultActionGroup,
    loadedTarget: BuildTargetId?,
  ) {
    group.addSeparator(BspPluginBundle.message("widget.loaded.target.separator.title"))

    if (loadedTarget != null) {
      group.addAction(LoadTargetAction(loadedTarget) { update() })
    }
  }

  private fun updateActionGroupWithAvailableTargetsSection(
    group: DefaultActionGroup,
    notLoadedTargetsIds: List<BuildTargetId>,
  ) {
    group.addSeparator(BspPluginBundle.message("widget.available.targets.to.load"))

    val actions = notLoadedTargetsIds.map { LoadTargetAction(it) { update() } }
    group.addAll(actions)
  }

  private fun getDocumentDetails(file: VirtualFile): DocumentTargetsDetails? {
    return when (URI.create(file.url).scheme) {
      // Could be also "jar"
      "file" -> magicMetaModelService.value.getTargetsDetailsForDocument(TextDocumentIdentifier(file.url))
      else -> null
    }
  }

  override fun createInstance(project: Project): StatusBarWidget =
    BspDocumentTargetsWidget(project)
}

public class BspDocumentTargetsWidgetFactory : StatusBarWidgetFactory {
  override fun getId(): String = ID

  override fun getDisplayName(): String =
    BspPluginBundle.message("widget.factory.display.name")

  override fun isAvailable(project: Project): Boolean =
    project.isBspProject

  override fun createWidget(project: Project): StatusBarWidget =
    BspDocumentTargetsWidget(project)

  override fun disposeWidget(widget: StatusBarWidget) {
    Disposer.dispose(widget)
  }

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean =
    true
}
