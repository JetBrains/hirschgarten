package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.magicmetamodel.MagicMetaModel
import org.jetbrains.plugins.bsp.services.InvalidTargetsProviderExtension
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TargetMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.SwingConstants

private class ListsUpdater(
  private val project: Project,
  private val updatePanelContent: (JComponent) -> Unit,
) {
  var targetPanel: BspPanelComponent
    private set
  val targetFilter = TargetFilter { rerenderComponents() }
  val searchBarPanel = SearchBarPanel()

  init {
    val assetExtension =
      BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
    val magicMetaModel = MagicMetaModelService.getInstance(project).value

    targetPanel =
      BspPanelComponent(
        iconProvider = assetExtension.obtainIconProvider(),
        buildToolId = project.buildToolId,
        toolName = assetExtension.presentableName,
        targets = magicMetaModel.getAllTargets(),
        searchBarPanel = searchBarPanel,
      ).also {
        it.addMouseListener { panel -> TargetMouseListener(panel, project) }
      }

    magicMetaModel.registerTargetLoadListener { rerenderComponents() }
  }

  private fun BuildToolAssetsExtension.obtainIconProvider(): Tristate<Icon> {
    return Tristate(
      loaded = loadedTargetIcon,
      unloaded = unloadedTargetIcon,
      invalid = invalidTargetIcon,
    )
  }

  private fun MagicMetaModel.getAllTargets(): Tristate.Targets {
    val invalidTargets =
      InvalidTargetsProviderExtension.ep.withBuildToolId(project.buildToolId)?.provideInvalidTargets(project).orEmpty()
    return Tristate.Targets(
      loaded = this.getAllLoadedTargets(),
      unloaded = this.getAllNotLoadedTargets(),
      invalid = invalidTargets.map { it.uri },
    )
  }

  fun rerenderComponents() {
    val allTargets = MagicMetaModelService.getInstance(project).value.getAllTargets()
    val filtered = targetFilter.filterTargets(allTargets)
    searchBarPanel.clearAllListeners()
    targetPanel = targetPanel.createNewWithTargets(
      targets = filtered,
      targetsFilteredOut = allTargets.size - filtered.size,
    )
    updatePanelContent(targetPanel.wrappedInScrollPane())
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {
  public constructor(project: Project) : this() {
    val actionManager = ActionManager.getInstance()
    val listsUpdater = ListsUpdater(project, this::setToolWindowContent)

    val actionGroup = actionManager
      .getAction("Bsp.ActionsToolbar") as DefaultActionGroup

    actionGroup.childActionsOrStubs.iterator().forEach {
      if (it.shouldBeDisposedAfterReload()) {
        actionGroup.remove(it)
      }
    }

    actionGroup.addSeparator()
    actionGroup.add(FilterActionGroup(listsUpdater.targetFilter))

    val actionToolbar = actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
    actionToolbar.targetComponent = this.component
    actionToolbar.orientation = SwingConstants.HORIZONTAL
    this.toolbar = actionToolbar.component

    setToolWindowContent(listsUpdater.targetPanel.wrappedInScrollPane())
  }

  private fun AnAction.shouldBeDisposedAfterReload(): Boolean =
    this is FilterActionGroup

  private fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
  }
}
