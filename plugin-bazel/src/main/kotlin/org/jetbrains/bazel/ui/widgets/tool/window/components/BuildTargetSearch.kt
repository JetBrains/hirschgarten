package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.NonUrgentExecutor
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bazel.ui.widgets.tool.window.model.BuildTargetsModel
import org.jetbrains.bsp.protocol.BuildTarget
import java.awt.Point
import java.util.concurrent.Callable
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class BuildTargetSearch(
  private val project: Project,
  private val model: BuildTargetsModel,
) : JBPanel<BuildTargetSearch>(VerticalLayout(0)) {

  val copyTargetIdAction: CopyTargetIdAction = object : CopyTargetIdAction.FromContainer(this) {
    override fun getTargetInfo(): BuildTarget? {
      return targetTree.getSelectedBuildTarget()
    }
  }

  private val targetTree = BuildTargetTree(
    project = project,
    model = model,
  )

  private val noResultsInfoComponent =
    JBLabel(
      BazelPluginBundle.message("widget.target.search.no.results"),
      SwingConstants.CENTER,
    )

  private var popupHandlerBuilder: ((BuildTargetContainer) -> PopupHandler)? = null

  init {
    noResultsInfoComponent.isVisible = false
    targetSearchPanel.add(noResultsInfoComponent)
    targetSearchPanel.add(targetTree.treeComponent)

    // Listen for model changes
    model.addListener {
      // Perform search if search is active and in progress
      if (model.isSearchActive && model.searchInProgress) {
        performSearch()
      }
      updateDisplay()
    }

    // Initial display update
    updateDisplay()
  }

  private fun performSearch() {
    val query = model.searchQuery
    val results = model.targets.filter { target ->
      // Search in target ID string representation
      val targetString = target.id.toShortString(project)
      query.containsMatchIn(targetString)
    }
    model.updateSearchResults(results)
  }

  private fun updateDisplay() {
    // Update the tree highlighter to highlight search matches
    if (model.isSearchActive) {
      targetTree.updateLabelHighlighter { QueryHighlighter.highlight(it, model.searchQuery) }
    } else {
      targetTree.updateLabelHighlighter { it }
    }

    // Update visibility of components based on search results
    noResultsInfoComponent.isVisible = model.isSearchActive && model.searchResults.isEmpty()
    targetTree.treeComponent.isVisible = !model.isSearchActive || model.searchResults.isNotEmpty()

    // Refresh the panel
    targetSearchPanel.revalidate()
    targetSearchPanel.repaint()
  }

  /**
   * @return `true` if search is in progress, or its results are ready,
   * `false` if nothing is currently being searched for
   */
  fun isSearchActive(): Boolean = model.isSearchActive

  override fun isEmpty(): Boolean = model.isEmpty

  override fun getComponent(): JComponent = targetSearchPanel

  override fun registerPopupHandler(popupHandlerBuilder: (BuildTargetContainer) -> PopupHandler) {
    this.popupHandlerBuilder = popupHandlerBuilder
    targetTree.registerPopupHandler(popupHandlerBuilder)
  }

  override fun getSelectedBuildTarget(): BuildTarget? =

  override fun getSelectedBuildTargetsUnderDirectory(): List<BuildTarget> = targetTree.getSelectedBuildTargetsUnderDirectory()

  override fun getSelectedComponentName(): String = targetTree.getSelectedComponentName()

  override fun isPointSelectable(point: Point): Boolean = targetTree.isPointSelectable(point)

  override fun selectTopTargetAndFocus() {
    targetTree.selectTopTargetAndFocus()
  }

