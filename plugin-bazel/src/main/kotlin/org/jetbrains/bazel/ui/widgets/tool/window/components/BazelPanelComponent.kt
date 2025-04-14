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
    val searchBarPanel = SearchBarPanel(model)
    val scrollPane = JBScrollPane(this)

    searchBarPanel.isEnabled = true
    searchBarPanel.registerSearchShortcutsOn(scrollPane)
    registerMoveDownShortcut(searchBarPanel)
    add(searchBarPanel, VerticalLayout.TOP)
    add(scrollPane, VerticalLayout.CENTER)
  }

  private fun registerMoveDownShortcut(searchBarPanel: SearchBarPanel) {
    val action = SimpleAction {
//      searchBarPanel.selectionRows = intArrayOf(0)
      requestFocus()
    }
    action.registerCustomShortcutSet(BspShortcuts.fromSearchBarToTargets, searchBarPanel)
  }
}
