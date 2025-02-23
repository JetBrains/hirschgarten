package org.jetbrains.bazel.ui.widgets.tool.window.search

import com.intellij.ui.PopupHandler
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.bazel.ui.widgets.tool.window.components.BuildTargetTree
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import java.awt.Point
import javax.swing.Icon
import javax.swing.JPanel

class LazySearchDisplay(icon: Icon, showAsTree: Boolean) {
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
    targetTree = targetTree.createNewWithTargetsAndHighlighter(targets, emptyList()) { QueryHighlighter.highlight(it, query) }
    component.add(targetTree.treeComponent)
  }

  fun registerPopupHandler(popupHandler: PopupHandler) {
    targetTree.registerPopupHandler { _ -> popupHandler }
  }

  fun getSelectedBuildTarget(): BuildTargetInfo? = targetTree.getSelectedBuildTarget()

  fun selectTopTargetAndFocus() {
    targetTree.selectTopTargetAndFocus()
  }

  fun isPointSelectable(point: Point): Boolean = targetTree.isPointSelectable(point)
}

private object QueryHighlighter {
  fun highlight(text: String, query: Regex): String {
    val matches = query.findAll(text).take(text.length).toList()
    return if (query.pattern.isNotEmpty() && matches.isNotEmpty()) {
      "<html>${highlightAllOccurrences(text, matches)}</html>"
    } else {
      text
    }
  }

  private fun highlightAllOccurrences(text: String, query: List<MatchResult>): String {
    val result = StringBuilder()
    var lastIndex = 0
    for (match in query) {
      val matchStart = match.range.first
      val matchEnd = match.range.last + 1
      result.append(text, lastIndex, matchStart)
      result.append("<b><u>")
      result.append(text, matchStart, matchEnd)
      result.append("</u></b>")
      lastIndex = matchEnd
    }
    result.append(text.substring(lastIndex))
    return result.toString()
  }
}
