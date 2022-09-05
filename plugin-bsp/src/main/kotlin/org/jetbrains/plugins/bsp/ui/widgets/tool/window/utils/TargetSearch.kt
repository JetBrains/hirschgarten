package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

private class TextChangeListener(val onUpdate: () -> Unit): DocumentListener {
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
  public var targets: Collection<BuildTarget> = emptyList()
  public var searchActive: Boolean = false
    private set

  public val searchBarComponent: JBTextField = JBTextField()
  private val searchListModel = DefaultListModel<BuildTarget>()
  public val searchListComponent: JBList<BuildTarget> = JBList(searchListModel)

  init {
    searchBarComponent.document.addDocumentListener(TextChangeListener(this::updateSearch))
    searchListComponent.selectionMode = ListSelectionModel.SINGLE_SELECTION
    searchListComponent.installCellRenderer {
      JBLabel(
        emphasizeQuery(it.displayName ?: it.id.uri),
        targetIcon,
        SwingConstants.LEFT
      )
    }
  }

  /**
   * Filters the build targets based on current search query. This method is executed each time
   * the search query is modified
   */
  private fun updateSearch() {
    val query = searchBarComponent.text
    this.searchActive = query.isNotEmpty()
    searchListModel.removeAllElements()
    onUpdate()
    if (searchActive) {
      searchListModel.addAll(targets.filter { (it.displayName ?: it.id.uri).contains(query, true) })
      searchListComponent.revalidate()
    }
  }

  /**
   * Emphasises text's name fragments containing search query
   * @param text the text to be processed
   * @return the text, wrapped in `<html>` tags, with all search query occurrences bolded and underlined
   */
  private fun emphasizeQuery(text: String): String {
    val query = searchBarComponent.text
    return "<html>${emphasizeQuery(text, "", query, 0)}</html>"
  }

  private tailrec fun emphasizeQuery(text: String, builtText: String, query: String, startIndex: Int): String {
    val foundIndex = text.indexOf(query, startIndex, true)
    if (foundIndex < 0) {
      return builtText + text.substring(startIndex)
    }
    val endFoundIndex = foundIndex + query.length
    val updatedText = builtText +
      text.substring(startIndex, foundIndex) +
      "<b><u>${text.substring(foundIndex, endFoundIndex)}</u></b>"
    return emphasizeQuery(text, updatedText, query, endFoundIndex)
  }
}