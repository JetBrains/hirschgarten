package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.concurrency.NonUrgentExecutor
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import java.util.concurrent.Callable
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

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

public class TargetSearch(private val targetIcon: Icon, private val onUpdate: () -> Unit) {
  public data class PrintableBuildTarget(
    val buildTarget: BuildTarget,
    var displayName: String = buildTarget.displayName ?: buildTarget.id.uri
  ) {
    override fun toString(): String = buildTarget.displayName ?: buildTarget.id.uri
  }

  public var targets: Collection<BuildTarget> = emptyList()
  public var isSearchActive: Boolean = false
    private set

  public val targetSearchPanel: JPanel = JPanel(VerticalLayout(0))

  private val searchListModel = DefaultListModel<PrintableBuildTarget>()

  public val searchBarComponent: JBTextField = JBTextField()
  private val inProgressInfoComponent = JBLabel(
    BspAllTargetsWidgetBundle.message("widget.loading.targets"),
    SwingConstants.CENTER
  )
  public val searchListComponent: JBList<PrintableBuildTarget> = JBList(searchListModel)
  private var showMoreButton = JButton("")

  init {
    searchBarComponent.document.addDocumentListener(TextChangeListener(this::updateSearch))
    searchListComponent.selectionMode = ListSelectionModel.SINGLE_SELECTION
    searchListComponent.installCellRenderer {targetWithString ->
      JPanel(VerticalLayout(0))
        .also {it.add(
          JBLabel(
            targetWithString.displayName,
            targetIcon,
            SwingConstants.LEFT
          )
        )
      }
    }
    inProgressInfoComponent.isVisible = false
    targetSearchPanel.add(inProgressInfoComponent)
    targetSearchPanel.add(searchListComponent)
  }

  /**
   * Filters the build targets based on current search query. Cancels all ongoing search operations.
   * This method is executed each time the search query is modified.
   */
  private fun updateSearch() {
    val query = searchBarComponent.text
    this.isSearchActive = query.isNotEmpty()
    targetSearchPanel.remove(showMoreButton)
    inProgressInfoComponent.isVisible = isSearchActive
    if (isSearchActive) conductSearch(query)
    onUpdate()
  }

  private fun conductSearch(query: String) {
    if (searchListModel.isEmpty) searchListComponent.isVisible = false
    ReadAction
      .nonBlocking(SearchCallable(query, targets))
      .finishOnUiThread(ModalityState.defaultModalityState(), ::maybeDisplaySearchResults)
      .coalesceBy(this)
      .submit(NonUrgentExecutor.getInstance())
  }

  private fun maybeDisplaySearchResults(result: Pair<String, List<PrintableBuildTarget>>) {
    if (result.first == currentQuery()) {
      searchListModel.removeAllElements()
      searchListModel.addAll(
        result.second.take(TARGETS_TO_HIGHLIGHT).map {
          PrintableBuildTarget(it.buildTarget, highlightQuery(it.displayName, currentQuery()))
        }
      )
      searchListComponent.isVisible = true
      addShowMoreButtonIfApplicable(result)
    }
    inProgressInfoComponent.isVisible = false
  }

  private fun addShowMoreButtonIfApplicable(result: Pair<String, List<PrintableBuildTarget>>) {
    if (result.second.size > TARGETS_TO_HIGHLIGHT) {
      showMoreButton = JButton("Show ${result.second.size - TARGETS_TO_HIGHLIGHT} more")
      showMoreButton.addActionListener {
        showMoreTargets(result.second)
      }
      targetSearchPanel.add(showMoreButton)
    }
  }

  /**
   * Shows the rest of searched build targets. Cancels all ongoing search operations.
   *
   * @param targets list of all build targets to be shown
   */
  private fun showMoreTargets(targets: List<PrintableBuildTarget>) {
    targetSearchPanel.remove(showMoreButton)
    searchListModel.removeAllElements()
    searchListModel.addAll(targets)
  }

  private fun currentQuery(): String = searchBarComponent.text

  public companion object {
    private const val TARGETS_TO_HIGHLIGHT: Int = 50

    private class SearchCallable(
      private val query: String,
      private val targets: Collection<BuildTarget>
    ) : Callable<Pair<String, List<PrintableBuildTarget>>> {
      override fun call(): Pair<String, List<PrintableBuildTarget>> {
        val searchedTargets = targets
          .filter { (it.displayName ?: it.id.uri).contains(query, true) }
          .map { target ->
            PrintableBuildTarget(
              target
            )
          }
        return Pair(query, searchedTargets)
      }
    }

    /**
     * Emphasises text's name fragments containing search query
     * @param text the text to be processed
     * @return the text, wrapped in `<html>` tags, with all search query occurrences bolded and underlined
     */
    private fun highlightQuery(text: String, query: String): String {
      return "<html>${highlightQuery(text, "", query, 0)}</html>"
    }

    private tailrec fun highlightQuery(text: String, builtText: String, query: String, startIndex: Int): String {
      val foundIndex = text.indexOf(query, startIndex, true)
      if (foundIndex < 0) {
        return builtText + text.substring(startIndex)
      }
      val endFoundIndex = foundIndex + query.length
      val updatedText = builtText +
        text.substring(startIndex, foundIndex) +
        "<b><u>${text.substring(foundIndex, endFoundIndex)}</u></b>"
      return highlightQuery(text, updatedText, query, endFoundIndex)
    }
  }
}
