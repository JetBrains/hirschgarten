package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import com.intellij.ui.PopupHandler
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetTree
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import javax.swing.Icon
import javax.swing.JPanel

class LazySearchDisplay(
  icon: Icon,
  buildToolId: BuildToolId,
  showAsTree: Boolean,
) {
  private val component: JPanel = JPanel(VerticalLayout(0))

  private var targets: List<BuildTargetInfo> = emptyList()
  private var query: Regex = "".toRegex()

  private var isOutdated = true

  private var targetTree: BuildTargetTree

  init {
    targetTree =
      BuildTargetTree(
        targetIcon = icon,
        invalidTargetIcon = icon,
        buildToolId = buildToolId,
        targets = emptyList(),
        invalidTargets = emptyList(),
        showAsList = !showAsTree,
      )
  }

  fun updateSearch(newTargets: List<BuildTargetInfo>, newQuery: Regex) {
    targets = newTargets
    query = newQuery
    isOutdated = true
  }

  fun get(): JPanel {
    rerenderIfOutdated()
    return component
  }

  private fun rerenderIfOutdated() {
    if (isOutdated) {
      rerender()
      isOutdated = false
    }
  }

  fun isEmpty(): Boolean = targets.isEmpty()

  private fun rerender() {
    component.remove(targetTree.treeComponent)
    targetTree = targetTree.createNewWithTargetsAndHighlighter(targets) { QueryHighlighter.highlight(it, query) }
    component.add(targetTree.treeComponent)
  }

  fun registerPopupHandler(popupHandler: PopupHandler) {
    targetTree.registerPopupHandler { _ -> popupHandler }
  }

  fun getSelectedBuildTarget(): BuildTargetInfo? = targetTree.getSelectedBuildTarget()

  fun selectTopTargetAndFocus() {
    targetTree.selectTopTargetAndFocus()
  }
}

private object QueryHighlighter {
  fun highlight(text: String, query: Regex): String =
    if (query.pattern.isNotEmpty() && query.containsMatchIn(text)) {
      "<html>${highlightAllOccurrences(text, query)}</html>"
    } else {
      text
    }

  private tailrec fun highlightAllOccurrences(
    text: String,
    query: Regex,
    builtText: String = "",
    startIndex: Int = 0,
  ): String {
    val foundRange =
      query.find(text, startIndex)?.range
        ?: return builtText + text.substring(startIndex)
    val updatedText =
      builtText +
        text.substring(startIndex, foundRange.first) +
        "<b><u>${text.substring(foundRange.first, foundRange.last + 1)}</u></b>"
    return highlightAllOccurrences(text, query, updatedText, foundRange.last + 1)
  }
}
