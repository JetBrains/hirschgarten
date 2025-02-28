package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Panel containing target tree and target search components, both containing the same collection of build targets.
 * `BspPanelComponent` extends [JPanel], which makes it possible to use it directly as a UI component
 */
class BspPanelComponent private constructor(
  private val targetIcon: Icon,
  private val toolName: String,
  private val targetTree: BuildTargetTree,
  private val targetSearch: BuildTargetSearch,
) : JPanel(VerticalLayout(0)) {
  private val emptyTreeMessage =
    JBLabel(
      BspPluginBundle.message("widget.no.targets.message"),
      SwingConstants.CENTER,
    )

  private var currentContainer: BuildTargetContainer? = null

  /**
   * @param targetIcon icon which will be shown next to valid build targets in this panel
   * @param invalidTargetIcon icon which will be shown next to invalid build targets in this panel
   * @param toolName name of the tool providing the build targets
   * @param targets collection of build targets this panel will contain
   * @param invalidTargets collection of invalid targets this panel will contain
   * @param searchBarPanel searchbar panel responsible for providing user's search queries
   */
  constructor(
    targetIcon: Icon,
    invalidTargetIcon: Icon,
    toolName: String,
    targets: Collection<BuildTargetInfo>,
    invalidTargets: List<BuildTargetIdentifier>,
    searchBarPanel: SearchBarPanel,
  ) : this(
    targetIcon = targetIcon,
    toolName = toolName,
    targetTree = BuildTargetTree(targetIcon, invalidTargetIcon, targets, invalidTargets),
    targetSearch = BuildTargetSearch(targetIcon, toolName, targets, searchBarPanel),
  )

  init {
    targetSearch.addQueryChangeListener { updatePanelContent() }
    updatePanelContent()
  }

  private fun updatePanelContent() {
    val newContainer = chooseNewContainer()
    val newPanelContent = newContainer?.getComponent() ?: emptyTreeMessage
    val oldPanelContent = getCurrentPanelContent()

    replacePanelContent(oldPanelContent, newPanelContent)
    currentContainer = newContainer
  }

  private fun chooseNewContainer(): BuildTargetContainer? =
    when {
      targetTree.isEmpty() -> null
      targetSearch.isSearchActive() -> targetSearch
      else -> targetTree
    }

  private fun getCurrentPanelContent(): Component? =
    try {
      this.getComponent(0)
    } catch (_: ArrayIndexOutOfBoundsException) {
      null
    }

  private fun replacePanelContent(oldContent: Component?, newContent: JComponent) {
    if (oldContent != newContent) {
      oldContent?.let { this.remove(it) }
      this.add(newContent)
      this.revalidate()
      this.repaint()
    }
  }

  fun withScrollAndSearch(): JPanel {
    val panel = JPanel(BorderLayout())
    val scrollPane = JBScrollPane(this)
    targetSearch.searchBarPanel.let {
      it.isEnabled = true
      it.registerSearchShortcutsOn(scrollPane)
      registerMoveDownShortcut(it)
      panel.add(it, BorderLayout.NORTH)
    }
    panel.add(scrollPane, BorderLayout.CENTER)
    return panel
  }

  private fun registerMoveDownShortcut(searchBarPanel: SearchBarPanel) {
    val action = SimpleAction { currentContainer?.selectTopTargetAndFocus() }
    action.registerCustomShortcutSet(BspShortcuts.fromSearchBarToTargets, searchBarPanel)
  }

  fun registerPopupHandler(popupHandlerBuilder: (BuildTargetContainer) -> PopupHandler) {
    targetTree.registerPopupHandler(popupHandlerBuilder)
    targetSearch.registerPopupHandler(popupHandlerBuilder)
  }

  /**
   * Creates a new panel with given targets. Mouse listeners added to target tree and target search components
   * will be copied using [BuildTargetContainer.createNewWithTargets]
   *
   * @param targets collection of build targets which the new panel will contain
   * @return newly created panel
   */
  fun createNewWithTargets(targets: Collection<BuildTargetInfo>, invalidTargets: List<BuildTargetIdentifier>): BspPanelComponent =
    BspPanelComponent(
      targetIcon,
      toolName,
      targetTree.createNewWithTargets(targets, invalidTargets),
      targetSearch.createNewWithTargets(targets, invalidTargets),
    )
}
