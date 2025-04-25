package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import java.awt.BorderLayout
import javax.swing.SwingConstants

class BazelTargetsPanel(private val project: Project, private val model: BazelTargetsPanelModel) :
  JBPanel<BazelTargetsPanel>(BorderLayout()) {
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
    add(searchBarPanel, BorderLayout.NORTH)
    showTargetTree()
    model.targetsPanel = this
  }

  private fun showTargetTree() {
    if (scrollPane !in components) {
      add(scrollPane, BorderLayout.CENTER)
    }
  }

  private fun hideTargetTree() {
    remove(scrollPane)
  }

  private fun showMessage() {
    if (message !in components) {
      add(message, BorderLayout.CENTER)
    }
  }

  private fun hideMessage() {
    remove(message)
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
      hideTargetTree()
      showMessage()
    } else if (model.isSearchActive && model.visibleTargets.isEmpty()) {
      message.text = BazelPluginBundle.message("widget.target.search.no.results")
      hideTargetTree()
      showMessage()
    } else {
      hideMessage()
      showTargetTree()
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
