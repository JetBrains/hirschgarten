package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.extension.points.buildToolIconProviderExtensions
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.StickyTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension.BspPanelCargoFeaturesComponent
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension.FeaturesService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension.FeaturesMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.NotLoadedTargetsMouseListener
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.SwingConstants

private enum class PanelShown {
  LOADED,
  NOTLOADED,
  CARGOFEATURES,
}

private class ListsUpdater(
  private val project: Project,
  toolName: String?,
  private val targetPanelUpdater: (ListsUpdater) -> Unit,
) {
  var loadedTargetsPanel: BspPanelComponent
    private set
  var notLoadedTargetsPanel: BspPanelComponent
    private set
  var cargoFeaturesPanel: BspPanelCargoFeaturesComponent? = null
    private set
  val targetFilter = TargetFilter { rerenderComponents() }
  val searchBarPanel = SearchBarPanel()

  init {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    val iconExtension = buildToolIconProviderExtensions().find { it.name() == toolName }
    val loadedTargetIcon = iconExtension?.icon() ?: BspPluginIcons.bsp

    loadedTargetsPanel =
      BspPanelComponent(loadedTargetIcon, toolName ?: "", magicMetaModel.getAllLoadedTargets(), searchBarPanel)
    loadedTargetsPanel.addMouseListener { LoadedTargetsMouseListener(it, project) }

    notLoadedTargetsPanel =
      BspPanelComponent(
        targetIcon = BspPluginIcons.notLoadedTarget,
        toolName = toolName ?: "",
        targets = magicMetaModel.getAllNotLoadedTargets(),
        searchBarPanel = searchBarPanel,
      )
    notLoadedTargetsPanel.addMouseListener { NotLoadedTargetsMouseListener(it, project) }
    magicMetaModel.registerTargetLoadListener { rerenderComponents() }

    // Create panel for Cargo features if server provides support for Cargo BSP extension
    // and therefore cargoFeatures are set in `MagicMetaModel`
    val featuresService = FeaturesService.getInstance(project)
    val bspConnection = BspConnectionService.getInstance(project).value
    val bool = bspConnection?.capabilities == null || bspConnection.capabilities?.cargoFeaturesProvider == true
      if (featuresService.areCargoFeaturesInitialized() && bool) {
        featuresService.value.getPackageFeatures().let { features ->
        cargoFeaturesPanel = BspPanelCargoFeaturesComponent(project, features)
        cargoFeaturesPanel!!.addMouseListener {  FeaturesMouseListener(it) }
      }
    }
  }

  fun rerenderComponents() {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    searchBarPanel.clearAllListeners()
    loadedTargetsPanel = loadedTargetsPanel.createNewWithTargets(targetFilter.getMatchingLoadedTargets(magicMetaModel))
    notLoadedTargetsPanel = notLoadedTargetsPanel.createNewWithTargets(
      targetFilter.getMatchingNotLoadedTargets(magicMetaModel))
    targetPanelUpdater(this@ListsUpdater)
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {
  private var panelShown = PanelShown.LOADED

  public constructor(project: Project) : this() {
    val actionManager = ActionManager.getInstance()
    val bspConnection = BspConnectionService.getInstance(project).value
    val listsUpdater = bspConnection?.buildToolId?.let {
      ListsUpdater(project, it, this::showCurrentPanel)
    }

    val actionGroup = actionManager
      .getAction("Bsp.ActionsToolbar") as DefaultActionGroup

    val notLoadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.not.loaded.targets.tab.name")
    val loadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.loaded.targets.tab.name")
    val cargoFeatures = BspAllTargetsWidgetBundle.message("widget.cargo.features.tab.name")

    actionGroup.childActionsOrStubs.iterator().forEach {
      if (it.shouldBeDisposedAfterReload()) {
        actionGroup.remove(it)
      }
    }

    actionGroup.addSeparator()

    if (listsUpdater != null) {
      actionGroup.add(StickyTargetAction(
        hintText = notLoadedTargetsActionName,
        icon = BspPluginIcons.notLoadedTarget,
        onPerform = { listsUpdater.showNotLoadedTargets() },
        selectionProvider = { panelShown == PanelShown.NOTLOADED },
      ))
      actionGroup.add(StickyTargetAction(
        hintText = loadedTargetsActionName,
        icon = BspPluginIcons.loadedTarget,
        onPerform = { listsUpdater.showLoadedTargets() },
        selectionProvider = { panelShown == PanelShown.LOADED },
      ))

      val featuresService = FeaturesService.getInstance(project)
      val cargoFeaturesProviderOrNoCapabilities = bspConnection.capabilities == null || bspConnection.capabilities?.cargoFeaturesProvider == true
      if (featuresService.areCargoFeaturesInitialized() && cargoFeaturesProviderOrNoCapabilities) {
        actionGroup.add(StickyTargetAction(
          hintText = cargoFeatures,
          icon = BspPluginIcons.cargoFeatures,
          onPerform = { listsUpdater.showCargoFeatures() },
          selectionProvider = { panelShown == PanelShown.CARGOFEATURES },
        ))
      }

      actionGroup.addSeparator()
      actionGroup.add(FilterActionGroup(listsUpdater.targetFilter))
    } else {
      actionGroup.addDummyActions()
    }

    val actionToolbar = actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
    actionToolbar.targetComponent = this.component
    actionToolbar.setOrientation(SwingConstants.HORIZONTAL)
    this.toolbar = actionToolbar.component

    listsUpdater?.let { showCurrentPanel(it) }
  }

  private fun AnAction.shouldBeDisposedAfterReload(): Boolean {
    val notLoadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.not.loaded.targets.tab.name")
    val loadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.loaded.targets.tab.name")
    val cargoFeaturesActionName = BspAllTargetsWidgetBundle.message("widget.cargo.features.tab.name")
    val restartActionName = BspAllTargetsWidgetBundle.message("restart.action.text")

    return this.templateText == notLoadedTargetsActionName ||
      this.templateText == loadedTargetsActionName ||
      this.templateText == cargoFeaturesActionName || 
      this.templateText == restartActionName ||
      this is EternallyDisabledAction ||
      this is FilterActionGroup
  }

  private fun ListsUpdater.showLoadedTargets() {
    panelShown = PanelShown.LOADED
    showCurrentPanel(this)
  }

  private fun ListsUpdater.showNotLoadedTargets() {
    panelShown = PanelShown.NOTLOADED
    showCurrentPanel(this)
  }

  private fun ListsUpdater.showCargoFeatures() {
    panelShown = PanelShown.CARGOFEATURES
    showCurrentPanel(this)
  }

  private fun DefaultActionGroup.addDummyActions() {
    add(
      EternallyDisabledAction(
        BspAllTargetsWidgetBundle.message("widget.not.loaded.targets.tab.name"),
        BspPluginIcons.notLoadedTarget,
      ),
    )
    add(
      EternallyDisabledAction(
        BspAllTargetsWidgetBundle.message("widget.loaded.targets.tab.name"),
        BspPluginIcons.loadedTarget,
      ),
    )
    addSeparator()
    add(
      EternallyDisabledAction(
        BspAllTargetsWidgetBundle.message("widget.filter.action.group"),
        AllIcons.General.Filter,
      ),
    )
    // I don't show Cargo Features panel in default actions, should I?
  }

  private fun showCurrentPanel(listsUpdater: ListsUpdater) {
    when (panelShown) {
      PanelShown.LOADED -> listsUpdater.loadedTargetsPanel
      PanelShown.NOTLOADED -> listsUpdater.notLoadedTargetsPanel
      PanelShown.CARGOFEATURES -> listsUpdater.cargoFeaturesPanel!!
    }.let { setToolWindowContent(it.wrappedInScrollPane()) }
  }

  private fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
  }
}

private class EternallyDisabledAction(hintText: String, icon: Icon) : AnAction({ hintText }, icon) {
  override fun actionPerformed(e: AnActionEvent) {
    // do nothing
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = false
  }

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.EDT
}
