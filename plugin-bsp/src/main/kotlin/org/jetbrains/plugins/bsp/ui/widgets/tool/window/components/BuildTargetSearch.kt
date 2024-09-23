package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.NonUrgentExecutor
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.BuildToolWindowTargetActionProviderExtension
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.LazySearchDisplay
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import java.util.concurrent.Callable
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class BuildTargetSearch(
  private val targetIcon: Icon,
  private val buildToolId: BuildToolId,
  private val toolName: String,
  targets: Collection<BuildTargetInfo>,
  val searchBarPanel: SearchBarPanel,
) : BuildTargetContainer {
  private val targetSearchPanel: JPanel = JPanel(VerticalLayout(0))

  override val copyTargetIdAction: CopyTargetIdAction = CopyTargetIdAction.FromContainer(this, targetSearchPanel)

  private val searchListDisplay = LazySearchDisplay(targetIcon, buildToolId, showAsTree = false)
  private val searchTreeDisplay = LazySearchDisplay(targetIcon, buildToolId, showAsTree = true)

  private var displayedSearchPanel: JPanel? = null
  private val noResultsInfoComponent =
    JBLabel(
      BspPluginBundle.message("widget.target.search.no.results"),
      SwingConstants.CENTER,
    )

  private val targets = targets.sortedBy { it.buildTargetName }

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

  override fun getSelectedBuildTarget(): BuildTargetInfo? = chooseTargetSearchPanel().getSelectedBuildTarget()

  override fun selectTopTargetAndFocus() {
    chooseTargetSearchPanel().selectTopTargetAndFocus()
  }

  override fun createNewWithTargets(newTargets: Collection<BuildTargetInfo>): BuildTargetSearch {
    val new = BuildTargetSearch(targetIcon, buildToolId, toolName, newTargets, searchBarPanel)
    popupHandlerBuilder?.let { new.registerPopupHandler(it) }
    return new
  }

  override fun getTargetActions(project: Project, buildTargetInfo: BuildTargetInfo): List<AnAction> =
    BuildToolWindowTargetActionProviderExtension.ep
      .withBuildToolId(project.buildToolId)
      ?.getTargetActions(targetSearchPanel, project, buildTargetInfo) ?: emptyList()

  private class SearchCallable(private val query: Regex, private val targets: Collection<BuildTargetInfo>) : Callable<SearchResults> {
    override fun call(): SearchResults =
      SearchResults(
        query,
        targets.filter { query.containsMatchIn(it.buildTargetName) },
      )
  }
}

private data class SearchResults(val query: Regex, val targets: List<BuildTargetInfo>)
