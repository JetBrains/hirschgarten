package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.NonUrgentExecutor
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolWindowTargetActionProviderExtension
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.LazySearchListDisplay
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.LazySearchTreeDisplay
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TargetNode
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate
import java.awt.Point
import java.awt.event.MouseListener
import java.util.concurrent.Callable
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

internal fun BuildTargetInfo.getBuildTargetName(): String =
  this.displayName ?: this.id

public class BuildTargetSearch(
  private val iconProvider: Tristate<Icon>,
  private val buildToolId: BuildToolId,
  private val toolName: String,
  private val targets: Tristate.Targets,
  public val searchBarPanel: SearchBarPanel,
) : BuildTargetContainer {
  public val targetSearchPanel: JPanel = JPanel(VerticalLayout(0))

  override val copyTargetIdAction: CopyTargetIdAction = CopyTargetIdAction(this, targetSearchPanel)

  private val searchListDisplay = LazySearchListDisplay(iconProvider)
  private val searchTreeDisplay = LazySearchTreeDisplay(iconProvider, buildToolId)

  private var displayedSearchPanel: JPanel? = null
  private val noResultsInfoComponent = JBLabel(
    BspPluginBundle.message("widget.target.search.no.results"),
    SwingConstants.CENTER,
  )

  private val mouseListenerBuilders = mutableSetOf<(BuildTargetContainer) -> MouseListener>()
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
  public fun isSearchActive(): Boolean = !searchBarPanel.isEmpty()

  private fun conductSearch(query: Regex) {
    if (isSearchActive()) {
      noResultsInfoComponent.isVisible = false
      searchBarPanel.inProgress = true
      ReadAction
        .nonBlocking(SearchCallable(query, targets))
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
    return if (chosenDisplay.isEmpty()) null
    else chosenDisplay.get().also { this.add(it) }
  }

  private fun chooseTargetSearchPanel() =
    if (searchBarPanel.isDisplayAsTreeChosen()) searchTreeDisplay else searchListDisplay

  override fun isEmpty(): Boolean = targets.isEmpty()

  override fun addMouseListener(listenerBuilder: (BuildTargetContainer) -> MouseListener) {
    mouseListenerBuilders.add(listenerBuilder)
    searchListDisplay.addMouseListener(listenerBuilder(this))
    searchTreeDisplay.addMouseListener(listenerBuilder(this))
  }

  /**
   * Adds a listener, which will be triggered every time the search query changes
   *
   * @param listener function to be triggered
   */
  public fun addQueryChangeListener(listener: () -> Unit) {
    queryChangeListeners.add(listener)
  }

  override fun getSelectedNode(): TargetNode? =
    chooseTargetSearchPanel().getSelectedNode()

  override fun createNewWithTargets(newTargets: Tristate.Targets): BuildTargetSearch {
    val new = BuildTargetSearch(iconProvider, buildToolId, toolName, newTargets, searchBarPanel)
    for (listenerBuilder in this.mouseListenerBuilders) {
      new.addMouseListener(listenerBuilder)
    }
    return new
  }

  // Fixes https://youtrack.jetbrains.com/issue/BAZEL-522
  public fun selectAtLocationIfListDisplayed(location: Point) {
    if (!searchBarPanel.isDisplayAsTreeChosen()) {
      searchListDisplay.selectAtLocation(location)
    }
  }

  override fun getComponent(): JComponent = targetSearchPanel

  private class SearchCallable(
    private val query: Regex,
    private val targets: Tristate.Targets,
  ) : Callable<SearchResults> {
    override fun call(): SearchResults =
      SearchResults(
        query,
        targets.filterByIds { query.containsMatchIn(it.getBuildTargetName()) },
      )
  }
}

private data class SearchResults(
  val query: Regex,
  val targets: Tristate.Targets,
)
