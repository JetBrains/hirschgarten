package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.NonUrgentExecutor
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bazel.ui.widgets.tool.window.search.LazySearchDisplay
import org.jetbrains.bazel.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.bsp.protocol.BuildTarget
import java.awt.Point
import java.util.concurrent.Callable
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class BuildTargetSearch(
  private val project: Project,
  private val targetIcon: Icon,
  private val toolName: String,
  targets: Collection<BuildTarget>,
  val searchBarPanel: SearchBarPanel,
) : BuildTargetContainer {
  private val targetSearchPanel: JPanel = JPanel(VerticalLayout(0))

  override val copyTargetIdAction: CopyTargetIdAction = CopyTargetIdAction.FromContainer(this, targetSearchPanel)

  private val searchListDisplay = LazySearchDisplay(project, targetIcon, showAsTree = false)
  private val searchTreeDisplay = LazySearchDisplay(project, targetIcon, showAsTree = true)

  private var displayedSearchPanel: JPanel? = null
  private val noResultsInfoComponent =
    JBLabel(
      BazelPluginBundle.message("widget.target.search.no.results"),
      SwingConstants.CENTER,
    )

  private val targets = targets.sortedBy { it.id.toShortString(project) }

  private var popupHandlerBuilder: ((BuildTargetContainer) -> PopupHandler)? = null
  private val queryChangeListeners = mutableSetOf<() -> Unit>()

  init {
    searchBarPanel.registerQueryChangeListener(::onSearchQueryChange)
    searchBarPanel.registerDisplayChangeListener { reloadPanels() }
    searchBarPanel.inProgress = false
    noResultsInfoComponent.isVisible = false
    targetSearchPanel.add(noResultsInfoComponent)
    onSearchQueryChange(getCurrentSearchQuery())
  }

  private fun onSearchQueryChange(newQuery: Regex) {
    conductSearch(newQuery)
    queryChangeListeners.forEach { it() }
  }

  private fun getCurrentSearchQuery(): Regex = searchBarPanel.getCurrentSearchQuery()

  /**
   * @return `true` if search is in progress, or its results are ready,
   * `false` if nothing is currently being searched for
   */
  fun isSearchActive(): Boolean = !searchBarPanel.isEmpty()

  private fun conductSearch(query: Regex) {
    if (isSearchActive()) {
      noResultsInfoComponent.isVisible = false
      searchBarPanel.inProgress = true
      ReadAction
        .nonBlocking(SearchCallable(project, query, targets))
        .finishOnUiThread(ModalityState.defaultModalityState()) { displaySearchResultsUnlessOutdated(it) }
        .coalesceBy(this)
        .submit(NonUrgentExecutor.getInstance())
    }
  }

  private fun displaySearchResultsUnlessOutdated(results: SearchResults) {
    if (results.query.pattern == getCurrentSearchQuery().pattern) {
      searchListDisplay.updateSearch(results.targets, results.query)
      searchTreeDisplay.updateSearch(results.targets, results.query)
      reloadPanels()
      searchBarPanel.inProgress = false
      noResultsInfoComponent.isVisible = results.targets.isEmpty()
    }
  }

  private fun reloadPanels() {
    if (getCurrentSearchQuery().pattern.isNotEmpty()) {
      displayedSearchPanel?.let { targetSearchPanel.remove(it) }
      displayedSearchPanel = targetSearchPanel.addLazySearchDisplayUnlessEmpty()
      targetSearchPanel.revalidate()
      targetSearchPanel.repaint()
    }
  }

  private fun JPanel.addLazySearchDisplayUnlessEmpty(): JPanel? {
    val chosenDisplay = chooseTargetSearchPanel()
    return if (chosenDisplay.isEmpty()) {
      null
    } else {
      chosenDisplay.get().also { this.add(it) }
    }
  }

  private fun chooseTargetSearchPanel() = if (searchBarPanel.isDisplayAsTreeChosen()) searchTreeDisplay else searchListDisplay

  override fun isEmpty(): Boolean = targets.isEmpty()

  override fun getComponent(): JComponent = targetSearchPanel

  override fun registerPopupHandler(popupHandlerBuilder: (BuildTargetContainer) -> PopupHandler) {
    this.popupHandlerBuilder = popupHandlerBuilder
    searchListDisplay.registerPopupHandler(popupHandlerBuilder(this))
    searchTreeDisplay.registerPopupHandler(popupHandlerBuilder(this))
  }

  /**
   * Adds a listener, which will be triggered every time the search query changes
   *
   * @param listener function to be triggered
   */
  fun addQueryChangeListener(listener: () -> Unit) {
    queryChangeListeners.add(listener)
  }

  override fun getSelectedBuildTarget(): BuildTarget? = chooseTargetSearchPanel().getSelectedBuildTarget()

  override fun getSelectedBuildTargetsUnderDirectory(): List<BuildTarget> =
    listOfNotNull(chooseTargetSearchPanel().getSelectedBuildTarget())

  override fun getSelectedComponentName(): String = chooseTargetSearchPanel().getSelectedBuildTarget()?.id?.toShortString(project) ?: ""

  override fun isPointSelectable(point: Point): Boolean = chooseTargetSearchPanel().isPointSelectable(point)

  override fun selectTopTargetAndFocus() {
    chooseTargetSearchPanel().selectTopTargetAndFocus()
  }

  override fun createNewWithTargets(newTargets: Collection<BuildTarget>, newInvalidTargets: List<Label>): BuildTargetSearch {
    val new = BuildTargetSearch(project, targetIcon, toolName, newTargets, searchBarPanel)
    popupHandlerBuilder?.let { new.registerPopupHandler(it) }
    return new
  }

  private class SearchCallable(
    private val project: Project,
    private val query: Regex,
    private val targets: Collection<BuildTarget>,
  ) : Callable<SearchResults> {
    override fun call(): SearchResults =
      SearchResults(
        query,
        targets.filter { query.containsMatchIn(it.id.toShortString(project)) },
      )
  }
}

private data class SearchResults(val query: Regex, val targets: List<BuildTarget>)
