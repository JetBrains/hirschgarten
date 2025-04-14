package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelToolwindowModel
import org.jetbrains.bazel.ui.widgets.tool.window.components.BuildTargetSearch
import org.jetbrains.bazel.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import javax.swing.SwingConstants

class BazelPanelComponent(private val project: Project, private val model: BazelToolwindowModel) :
  JBPanel<BazelPanelComponent>(VerticalLayout(0)) {
  val searchBarPanel = SearchBarPanel(model)
  val targetsPanel = BuildTargetSearch(project, model)
  val scrollPane = JBScrollPane(targetsPanel)

  private val emptyTreeMessage =
    JBLabel(
      BazelPluginBundle.message("widget.no.targets.message"),
      SwingConstants.CENTER,
    )

  init {
    searchBarPanel.isEnabled = true
    searchBarPanel.registerSearchShortcutsOn(scrollPane)
    registerMoveDownShortcut(searchBarPanel)
    add(searchBarPanel, VerticalLayout.TOP)
    add(scrollPane, VerticalLayout.CENTER)
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
