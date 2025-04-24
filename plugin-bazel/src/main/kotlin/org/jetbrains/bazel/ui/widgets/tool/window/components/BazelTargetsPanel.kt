package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import javax.swing.SwingConstants

class BazelTargetsPanel(private val project: Project, private val model: BazelTargetsPanelModel) :
  JBPanel<BazelTargetsPanel>(VerticalLayout(0)) {
  val searchBarPanel = SearchBarPanel(model)
  val targetTree =
    BuildTargetTree(
      project = project,
      model = model,
    )
  val scrollPane = JBScrollPane(targetTree)

  private val message =
    JBLabel(
      BazelPluginBundle.message("widget.no.targets.message"),
      SwingConstants.CENTER,
    )

  init {
    searchBarPanel.isEnabled = true
    searchBarPanel.registerSearchShortcutsOn(scrollPane)
    registerMoveDownShortcut(searchBarPanel)
    add(searchBarPanel, VerticalLayout.TOP)
    add(scrollPane, VerticalLayout.FILL)
    model.targetsPanel = this
  }

  fun update() {
    // Update the tree highlighter to highlight search matches
    val searchRegex = model.searchRegex
    if (searchRegex != null) {
      targetTree.cellRenderer = TargetTreeCellRenderer { QueryHighlighter.highlight(it, searchRegex) }
    } else {
      targetTree.cellRenderer = TargetTreeCellRenderer { it }
    }

    // Update visibility of components based on search results
    if (!model.hasAnyTargets) {
      message.text = BazelPluginBundle.message("widget.no.targets.message")
      message.isVisible = true
      targetTree.isVisible = false
    } else if (model.isSearchActive && model.visibleTargets.isEmpty()) {
      message.text = BazelPluginBundle.message("widget.target.search.no.results")
      message.isVisible = true
      targetTree.isVisible = false
    } else {
      message.isVisible = false
      targetTree.isVisible = true
    }

    targetTree.updateTree()

    // Refresh the panel
    revalidate()
    repaint()
  }

  private fun registerMoveDownShortcut(searchBarPanel: SearchBarPanel) {
    val action =
      SimpleAction {
//      searchBarPanel.selectionRows = intArrayOf(0)
        requestFocus()
      }
    action.registerCustomShortcutSet(BspShortcuts.fromSearchBarToTargets, searchBarPanel)
  }
}
