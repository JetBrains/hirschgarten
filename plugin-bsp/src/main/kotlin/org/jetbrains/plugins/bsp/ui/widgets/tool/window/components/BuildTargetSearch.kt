package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.NonUrgentExecutor
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolWindowTargetActionProviderExtension
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.LazySearchListDisplay
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.LazySearchTreeDisplay
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import java.awt.Point
import java.awt.event.MouseListener
import java.util.concurrent.Callable
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private fun BuildTargetInfo.getBuildTargetName(): String =
  this.displayName ?: this.id

public class BuildTargetSearch(
  private val targetIcon: Icon,
  private val buildToolId: BuildToolId,
  private val toolName: String,
  targets: Collection<BuildTargetInfo>,
  public val searchBarPanel: SearchBarPanel,
) : BuildTargetContainer {
  public val targetSearchPanel: JPanel = JPanel(VerticalLayout(0))

  override val copyTargetIdAction: CopyTargetIdAction = CopyTargetIdAction(this, targetSearchPanel)

  private val searchListDisplay = LazySearchListDisplay(targetIcon)
  private val searchTreeDisplay = LazySearchTreeDisplay(targetIcon, buildToolId)

  private var displayedSearchPanel: JPanel? = null
  private val inProgressInfoComponent = JBLabel(
    BspPluginBundle.message("widget.loading.targets"),
    SwingConstants.CENTER,
  )
  private val noResultsInfoComponent = JBLabel(
    BspPluginBundle.message("widget.target.search.no.results"),
    SwingConstants.CENTER,
  )

  private val targets = targets.sortedBy { it.getBuildTargetName() }

  private val mouseListenerBuilders = mutableSetOf<(BuildTargetContainer) -> MouseListener>()
  private val queryChangeListeners = mutableSetOf<() -> Unit>()

  init {
    attachSearchBarTextChangeListener { onSearchQueryChange() }
    searchBarPanel.registerDisplayChangeListener { reloadPanels() }
    inProgressInfoComponent.isVisible = false
    targetSearchPanel.add(inProgressInfoComponent)
    noResultsInfoComponent.isVisible = false
    targetSearchPanel.add(noResultsInfoComponent)
    onSearchQueryChange()
  }

  private fun attachSearchBarTextChangeListener(onUpdate: () -> Unit) {
    searchBarPanel.addTextFieldDocumentListener(TextChangeListener(onUpdate))
  }

  private fun onSearchQueryChange() {
    val currentQuery = getCurrentSearchQuery()
    maybeConductSearch(currentQuery)
    queryChangeListeners.forEach { it() }
  }

  private fun getCurrentSearchQuery(): String = searchBarPanel.getCurrentSearchQuery()

  /**
   * @return `true` if search is in progress or its results are ready,
   * `false` if nothing is currently being searched for
   */
  public fun isSearchActive(): Boolean = getCurrentSearchQuery().isNotEmpty()

  private fun maybeConductSearch(query: String) {
    if (isSearchActive()) {
      noResultsInfoComponent.isVisible = false
      inProgressInfoComponent.isVisible = true
      ReadAction
        .nonBlocking(SearchCallable(query, targets))
        .finishOnUiThread(ModalityState.defaultModalityState()) { displaySearchResultsUnlessOutdated(it) }
        .coalesceBy(this)
        .submit(NonUrgentExecutor.getInstance())
    }
  }

  private fun displaySearchResultsUnlessOutdated(results: SearchResults) {
    if (results.query == getCurrentSearchQuery()) {
      searchListDisplay.updateSearch(results.targets, results.query)
      searchTreeDisplay.updateSearch(results.targets, results.query)
      reloadPanels()
      inProgressInfoComponent.isVisible = false
      noResultsInfoComponent.isVisible = results.targets.isEmpty()
    }
  }

  private fun reloadPanels() {
    if (getCurrentSearchQuery().isNotEmpty()) {
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

  override fun getSelectedBuildTarget(): BuildTargetInfo? =
    chooseTargetSearchPanel().getSelectedBuildTarget()

  override fun createNewWithTargets(newTargets: Collection<BuildTargetInfo>): BuildTargetSearch {
    val new = BuildTargetSearch(targetIcon, buildToolId, toolName, newTargets, searchBarPanel)
    for (listenerBuilder in this.mouseListenerBuilders) {
      new.addMouseListener(listenerBuilder)
    }
    return new
  }

  // https://youtrack.jetbrains.com/issue/BAZEL-522
  public fun selectAtLocationIfListDisplayed(location: Point) {
    if (!searchBarPanel.isDisplayAsTreeChosen()) {
      searchListDisplay.selectAtLocation(location)
    }
  }

  override fun getTargetActions(project: Project, buildTargetInfo: BuildTargetInfo): List<AnAction> =
    BuildToolWindowTargetActionProviderExtension.ep.withBuildToolId(project.buildToolId)
      ?.getTargetActions(targetSearchPanel, project, buildTargetInfo) ?: emptyList()

  private class SearchCallable(
    private val query: String,
    private val targets: Collection<BuildTargetInfo>,
  ) : Callable<SearchResults> {
    override fun call(): SearchResults =
      SearchResults(
        query,
        targets.filter { it.getBuildTargetName().contains(query, true) },
      )
  }
}

private class TextChangeListener(val onUpdate: () -> Unit) : DocumentListener {
  override fun insertUpdate(e: DocumentEvent?) {
    onUpdate()
  }

  override fun removeUpdate(e: DocumentEvent?) {
    onUpdate()
  }

  override fun changedUpdate(e: DocumentEvent?) {
    onUpdate()
  }
}

private data class SearchResults(
  val query: String,
  val targets: List<BuildTargetInfo>,
)
