package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.NonUrgentExecutor
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import java.awt.event.MouseListener
import java.util.concurrent.Callable
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private const val TARGETS_TO_HIGHLIGHT: Int = 50

private fun BuildTarget.getBuildTargetName(): String =
  this.displayName ?: this.id.uri

public class BuildTargetSearch(
  private val targetIcon: Icon,
  targets: Collection<BuildTarget>
) : BuildTargetContainer {

  public val searchBarComponent: JBTextField = JBTextField()
  public val targetSearchPanel: JPanel = JPanel(VerticalLayout(0))

  private val targets = targets.sortedBy { it.getBuildTargetName() }
  private val searchListModel = DefaultListModel<PrintableBuildTarget>()
  private val searchListComponent = JBList(searchListModel)
  private val inProgressInfoComponent = JBLabel(
    BspAllTargetsWidgetBundle.message("widget.loading.targets"),
    SwingConstants.CENTER
  )
  private var showMoreButton = JButton("")
  private val mouseListenerBuilders = mutableSetOf<(BuildTargetContainer) -> MouseListener>()
  private val queryChangeListeners = mutableSetOf<() -> Unit>()

  init {
    attachSearchBarTextChangeListener(::onSearchQueryChange)
    searchListComponent.selectionMode = ListSelectionModel.SINGLE_SELECTION
    searchListComponent.installCellRenderer(::renderSearchListCell)
    inProgressInfoComponent.isVisible = false
    targetSearchPanel.add(inProgressInfoComponent)
    targetSearchPanel.add(searchListComponent)
  }

  private fun attachSearchBarTextChangeListener(onUpdate: () -> Unit) {
    searchBarComponent.document.addDocumentListener(TextChangeListener(onUpdate))
  }

  private fun onSearchQueryChange() {
    val currentQuery = getCurrentSearchQuery()
    cleanPanelElements()
    maybeConductSearch(currentQuery)
    queryChangeListeners.forEach { it() }
  }

  private fun getCurrentSearchQuery(): String = searchBarComponent.text

  private fun cleanPanelElements() {
    targetSearchPanel.remove(showMoreButton)
    inProgressInfoComponent.isVisible = isSearchActive()
    searchListComponent.isVisible = !searchListModel.isEmpty || !isSearchActive()
  }

  /**
   * @return `true` if search is in progress or its results are ready,
   * `false` if nothing is currently being searched for
   */
  public fun isSearchActive(): Boolean = searchBarComponent.text.isNotEmpty()

  private fun renderSearchListCell(printableBuildTarget: PrintableBuildTarget): JPanel {
    val renderedCell = JPanel(VerticalLayout(0))
    renderedCell.add(
      JBLabel(
        printableBuildTarget.displayName,
        targetIcon,
        SwingConstants.LEFT
      )
    )
    return renderedCell
  }

  private fun maybeConductSearch(query: String) {
    if (isSearchActive()) {
      ReadAction
        .nonBlocking(SearchCallable(query, targets))
        .finishOnUiThread(ModalityState.defaultModalityState(), ::displaySearchResultsUnlessOutdated)
        .coalesceBy(this)
        .submit(NonUrgentExecutor.getInstance())
    }
  }

  private fun displaySearchResultsUnlessOutdated(results: SearchResults) {
    if (results.query == getCurrentSearchQuery()) {
      replaceSearchListElementsWith(takeSomeTargetsAndHighlight(results.targets))
      searchListComponent.isVisible = true
      maybeAddShowMoreButton(results.targets)
    }
    inProgressInfoComponent.isVisible = false
  }

  private fun replaceSearchListElementsWith(printableTargets: Collection<PrintableBuildTarget>) {
    searchListModel.removeAllElements()
    searchListModel.addAll(printableTargets)
  }

  private fun takeSomeTargetsAndHighlight(targets: Collection<BuildTarget>): List<PrintableBuildTarget> =
    targets.take(TARGETS_TO_HIGHLIGHT).map {
      PrintableBuildTarget(
        it,
        QueryHighlighter.highlight(it.getBuildTargetName(), getCurrentSearchQuery())
      )
    }

  private fun maybeAddShowMoreButton(targets: Collection<BuildTarget>) {
    val remainingTargets = targets.size - TARGETS_TO_HIGHLIGHT
    if (remainingTargets > 0) {
      showMoreButton = JButton("Show $remainingTargets more")
      showMoreButton.addActionListener {
        showMoreTargets(targets)
      }
      targetSearchPanel.add(showMoreButton)
    }
  }

  private fun showMoreTargets(targets: Collection<BuildTarget>) {
    targetSearchPanel.remove(showMoreButton)
    replaceSearchListElementsWith(targets.map { PrintableBuildTarget(it) })
  }

  override fun isEmpty(): Boolean = targets.isEmpty()

  override fun addMouseListener(listenerBuilder: (BuildTargetContainer) -> MouseListener) {
    mouseListenerBuilders.add(listenerBuilder)
    searchListComponent.addMouseListener(listenerBuilder(this))
  }

  /**
   * Adds a listener, which will be triggered every time the search query changes
   *
   * @param listener function to be triggered
   */
  public fun addQueryChangeListener(listener: () -> Unit) {
    queryChangeListeners.add(listener)
  }

  override fun getSelectedBuildTarget(): BuildTarget? {
    val selected = searchListComponent.selectedValue
    return selected?.buildTarget
  }

  override fun createNewWithTargets(newTargets: Collection<BuildTarget>): BuildTargetSearch {
    val new = BuildTargetSearch(targetIcon, newTargets)
    for (listenerBuilder in this.mouseListenerBuilders) {
      new.addMouseListener(listenerBuilder)
    }
    return new
  }

  public data class PrintableBuildTarget(
    val buildTarget: BuildTarget,
    var displayName: String = buildTarget.getBuildTargetName()
  ) {
    override fun toString(): String = buildTarget.displayName ?: buildTarget.id.uri
  }

  private class SearchCallable(
    private val query: String,
    private val targets: Collection<BuildTarget>
  ) : Callable<SearchResults> {
    override fun call(): SearchResults =
      SearchResults(
        query,
        targets.filter { it.getBuildTargetName().contains(query, true) }
      )
  }
}

private object QueryHighlighter {
  fun highlight(text: String, query: String): String =
    "<html>${highlightAllOccurrences(text, query)}</html>"

  private tailrec fun highlightAllOccurrences(text: String, query: String, builtText: String = "", startIndex: Int = 0): String {
    val foundIndex = text.indexOf(query, startIndex, true)
    if (foundIndex < 0) {
      return builtText + text.substring(startIndex)
    }
    val endFoundIndex = foundIndex + query.length
    val updatedText = builtText +
      text.substring(startIndex, foundIndex) +
      "<b><u>${text.substring(foundIndex, endFoundIndex)}</u></b>"
    return highlightAllOccurrences(text, query, updatedText, endFoundIndex)
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
  val targets: List<BuildTarget>
)
