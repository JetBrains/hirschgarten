package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TargetNode
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate
import java.awt.event.MouseListener
import javax.swing.JPanel

public abstract class LazySearchDisplay {
  protected val component: JPanel = JPanel(VerticalLayout(0))

  protected var targets: Tristate.Targets = Tristate.Targets.EMPTY
  protected var query: Regex = "".toRegex()

  private var isOutdated = true

  public fun updateSearch(newTargets: Tristate.Targets, newQuery: Regex) {
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

  public abstract fun getSelectedNode(): TargetNode?

  protected object QueryHighlighter {
    public fun highlight(text: String, query: Regex): String =
      if (query.pattern.isNotEmpty() && query.containsMatchIn(text)) {
        "<html>${highlightAllOccurrences(text, query)}</html>"
      } else text

    private tailrec fun highlightAllOccurrences(
      text: String,
      query: Regex,
      builtText: String = "",
      startIndex: Int = 0,
    ): String {
      val foundRange = query.find(text, startIndex)?.range
        ?: return builtText + text.substring(startIndex)
      val updatedText = builtText +
        text.substring(startIndex, foundRange.first) +
        "<b><u>${text.substring(foundRange.first, foundRange.last + 1)}</u></b>"
      return highlightAllOccurrences(text, query, updatedText, foundRange.last + 1)
    }
  }
}
