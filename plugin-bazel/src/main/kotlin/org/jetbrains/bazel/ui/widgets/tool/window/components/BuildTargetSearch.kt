package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import javax.swing.SwingConstants

class BuildTargetSearch(private val project: Project, private val model: BazelToolwindowModel) :
  JBPanel<BuildTargetSearch>(VerticalLayout(0)) {
  private val targetTree =
    BuildTargetTree(
      project = project,
      model = model,
    )

  private val noResultsInfoComponent =
    JBLabel(
      BazelPluginBundle.message("widget.target.search.no.results"),
      SwingConstants.CENTER,
    )

  init {
    noResultsInfoComponent.isVisible = false
    add(noResultsInfoComponent)
    add(targetTree)
    model.buildTargetTree = targetTree

    // Listen for model changes
//    model.addListener {
//      // Perform search if search is active and in progress
//      if (model.isSearchActive && model.searchInProgress) {
//        performSearch()
//      }
//      updateDisplay()
//    }

    // Initial display update
    updateDisplay()
  }

  private fun performSearch() {
    val query = model.regex
    val results =
      model.targets.keys
        .toList()
        .map { Pair(it.toShortString(project), it) }
        .filter {
          // Search in target ID string representation
          val targetString = it.first
          query.containsMatchIn(targetString)
        }.sortedBy { it.first }
        .map { it.second }
    model.updateSearchResults(results)
  }

  private fun updateDisplay() {
    // Update the tree highlighter to highlight search matches
    if (model.isSearchActive) {
      targetTree.cellRenderer = TargetTreeCellRenderer { QueryHighlighter.highlight(it, model.regex) }
    } else {
      targetTree.cellRenderer = TargetTreeCellRenderer { it }
    }

    // Update visibility of components based on search results
    noResultsInfoComponent.isVisible = model.isSearchActive && model.searchResults.isEmpty()
    targetTree.isVisible = !model.isSearchActive || model.searchResults.isNotEmpty()

    // Refresh the panel
    revalidate()
    repaint()
  }
}
