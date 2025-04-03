package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import org.jetbrains.bsp.protocol.BuildTarget
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
  private val project: Project,
  private val targetIcon: Icon,
  private val toolName: String,
  private val targetTree: BuildTargetTree,
  private val targetSearch: BuildTargetSearch,
) : JPanel(VerticalLayout(0)) {
  private val emptyTreeMessage =
    JBLabel(
      BazelPluginBundle.message("widget.no.targets.message"),
      SwingConstants.CENTER,
    )

  private var currentContainer: BuildTargetContainer? = null

  constructor(
    project: Project,
    targetIcon: Icon,
    invalidTargetIcon: Icon,
    toolName: String,
    targets: Collection<BuildTarget>,
    invalidTargets: List<Label>,
    searchBarPanel: SearchBarPanel,
  ) : this(
    project = project,
    targetIcon = targetIcon,
    toolName = toolName,
    targetTree = BuildTargetTree(project, targetIcon, invalidTargetIcon, targets, invalidTargets),
    targetSearch = BuildTargetSearch(project, targetIcon, toolName, targets, searchBarPanel),
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
  fun createNewWithTargets(targets: Collection<BuildTarget>, invalidTargets: List<Label>): BspPanelComponent =
    BspPanelComponent(
      project,
      targetIcon,
      toolName,
      targetTree.createNewWithTargets(targets, invalidTargets),
      targetSearch.createNewWithTargets(targets, invalidTargets),
    )
}
