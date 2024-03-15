package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate
import java.awt.Component
import java.awt.event.MouseListener
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Panel containing target tree and target search components, both containing the same collection of build targets.
 * `BspPanelComponent` extends [JPanel], which makes it possible to use it directly as a UI component
 */
public class BspPanelComponent private constructor(
  private val iconProvider: Tristate<Icon>,
  private val toolName: String,
  private val targetTree: BuildTargetTree,
  private val targetSearch: BuildTargetSearch,
  private val targetsFilteredOut: Int,
) : JPanel(VerticalLayout(0)) {
  private val emptyTreeMessage = JBLabel(
    BspPluginBundle.message("widget.no.targets.message"),
    SwingConstants.CENTER,
  )
  private val hiddenTargetsMessage = JBLabel(
    BspPluginBundle.message("widget.filter.hidden.targets", targetsFilteredOut),
    SwingConstants.CENTER,
  ).apply { isEnabled = false }

  /**
   * @param iconProvider provider for icons to be shown next to targets in this panel
   * @param buildToolId id of the build tool
   * @param toolName name of the tool providing the build targets
   * @param targets all targets to be shown
   * @param searchBarPanel searchbar panel responsible for providing user's search queries
   */
  public constructor(
    iconProvider: Tristate<Icon>,
    buildToolId: BuildToolId,
    toolName: String,
    targets: Tristate.Targets,
    searchBarPanel: SearchBarPanel,
  ) : this(
    iconProvider = iconProvider,
    toolName = toolName,
    targetTree = BuildTargetTree(iconProvider, buildToolId, targets),
    targetSearch = BuildTargetSearch(iconProvider, buildToolId, toolName, targets, searchBarPanel),
    targetsFilteredOut = 0,
  )

  init {
    targetSearch.addQueryChangeListener { onSearchQueryUpdate() }
    replacePanelContent(null, chooseNewContent())
  }

  private fun onSearchQueryUpdate() {
    val newPanelContent = chooseNewContent()
    val oldPanelContent = getCurrentContent()

    if (newPanelContent != oldPanelContent) {
      replacePanelContent(oldPanelContent, newPanelContent)
    }
  }

  private fun chooseNewContent(): JComponent =
    when {
      targetTree.isEmpty() -> emptyTreeMessage
      targetSearch.isSearchActive() -> targetSearch.targetSearchPanel
      else -> targetTree.treeComponent
    }

  private fun getCurrentContent(): Component? =
    try {
      this.getComponent(0)
    } catch (_: ArrayIndexOutOfBoundsException) {
      log.warn("Sidebar widget panel does not have enough children")
      null
    }

  private fun replacePanelContent(oldContent: Component?, newContent: JComponent) {
    oldContent?.let { this.remove(it) }
    this.add(newContent)
    if (targetsFilteredOut > 0) this.add(hiddenTargetsMessage)
    this.revalidate()
    this.repaint()
  }

  public fun wrappedInScrollPane(): JBScrollPane {
    val scrollPane = JBScrollPane(this)
    targetSearch.searchBarPanel.let {
      it.isEnabled = true
      it.registerShortcutsOn(scrollPane)
      scrollPane.setColumnHeaderView(it)
    }
    return scrollPane
  }

  /**
   * Adds a mouse listener to this panel's target tree and target search components
   *
   * @param listenerBuilder mouse listener builder
   */
  public fun addMouseListener(listenerBuilder: (BuildTargetContainer) -> MouseListener) {
    targetTree.addMouseListener(listenerBuilder)
    targetSearch.addMouseListener(listenerBuilder)
  }

  /**
   * Creates a new panel with given targets. Mouse listeners added to target tree and target search components
   * will be copied using [BuildTargetContainer.createNewWithTargets]
   *
   * @param targets collection of build targets which the new panel will contain
   * @param targetsFilteredOut number of targets which were removed when applying filters
   * @return newly created panel
   */
  public fun createNewWithTargets(
    targets: Tristate.Targets,
    targetsFilteredOut: Int,
  ): BspPanelComponent =
    BspPanelComponent(
      iconProvider,
      toolName,
      targetTree.createNewWithTargets(targets),
      targetSearch.createNewWithTargets(targets),
      targetsFilteredOut,
    )

  private companion object {
    private val log = logger<BspPanelComponent>()
  }
}
