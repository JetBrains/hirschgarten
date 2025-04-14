package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.components.JBComponent
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.ui.widgets.tool.window.model.BuildTargetsModel
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
class BspPanelComponent(
  private val project: Project,
  private val model: BuildTargetsModel,
) : JBPanel<BspPanelComponent>(VerticalLayout(0)) {
  private val emptyTreeMessage =
    JBLabel(
      BazelPluginBundle.message("widget.no.targets.message"),
      SwingConstants.CENTER,
    )


  init {
    // Listen for model changes
    model.addListener {
      updatePanelContent()
    }
    
    // Initial panel content update
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
      model.isEmpty -> null
      model.isSearchActive -> targetSearch
      else -> targetTree
    }

  private fun replacePanelContent(oldContent: Component?, newContent: JComponent) {
    if (oldContent != newContent) {
      oldContent?.let { this.remove(it) }
      this.add(newContent)
      this.revalidate()
      this.repaint()
    }
  }

  fun withScrollAndSearch(searchBarPanel: SearchBarPanel): JPanel {
    val panel = JPanel(BorderLayout())
    val scrollPane = JBScrollPane(this)
    
    searchBarPanel.isEnabled = true
    searchBarPanel.registerSearchShortcutsOn(scrollPane)
    registerMoveDownShortcut(searchBarPanel)
    panel.add(searchBarPanel, BorderLayout.NORTH)
    
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
   * Updates the panel with new targets by updating the model
   *
   * @param targets collection of build targets which the panel will contain
   * @param invalidTargets collection of invalid targets which the panel will contain
   * @return this panel instance
   */
  fun updateTargets(targets: Collection<BuildTarget>, invalidTargets: List<Label>): BspPanelComponent {
    model.updateTargets(targets, invalidTargets)
    return this
  }
}
