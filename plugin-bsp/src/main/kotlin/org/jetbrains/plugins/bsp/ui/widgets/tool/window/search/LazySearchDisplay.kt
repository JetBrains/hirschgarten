package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.ui.components.panels.VerticalLayout
import java.awt.event.MouseListener
import javax.swing.*

public abstract class LazySearchDisplay {
  protected val component: JPanel = JPanel(VerticalLayout(0))

  protected var targets: List<BuildTarget> = emptyList()
  protected var query: String = ""

  private var isOutdated = true

  public fun updateSearch(newTargets: List<BuildTarget>, newQuery: String) {
    targets = newTargets
    query = newQuery
    isOutdated = true
  }

  public fun get(): JPanel {
    rerenderIfOutdated()
    return component
  }

  private fun rerenderIfOutdated() {
    if (isOutdated) {
      rerender()
      isOutdated = false
    }
  }

  public fun isEmpty(): Boolean = targets.isEmpty()

  protected abstract fun rerender()

  public abstract fun addMouseListener(mouseListener: MouseListener)

  public abstract fun getSelectedBuildTarget(): BuildTarget?

  protected data class PrintableBuildTarget(
    val buildTarget: BuildTarget,
    var displayName: String = buildTarget.let { it.displayName ?: it.id.uri }
  ) {
    override fun toString(): String = buildTarget.displayName ?: buildTarget.id.uri
  }

  protected object QueryHighlighter {
    public fun highlight(text: String, query: String): String =
      if (query.isNotEmpty() && text.contains(query, true)) {
        "<html>${highlightAllOccurrences(text, query)}</html>"
      }
      else text

    private tailrec fun highlightAllOccurrences(
      text: String,
      query: String,
      builtText: String = "",
      startIndex: Int = 0
    ): String {
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
}
