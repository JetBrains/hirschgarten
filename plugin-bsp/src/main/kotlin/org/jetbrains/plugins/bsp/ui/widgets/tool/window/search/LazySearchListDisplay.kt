package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TargetNode
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate
import java.awt.Point
import java.awt.event.MouseListener
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.ListSelectionModel

private const val MAX_TARGETS_TO_HIGHLIGHT: Int = 50

public class LazySearchListDisplay(
  private val iconProvider: Tristate<Icon>,
) : LazySearchDisplay() {
  private val searchListModel = DefaultListModel<TargetNode.Target>()
  private val searchListComponent = JBList(searchListModel)
  private var showMoreButton = JButton("")

  private val queryHighlighter: (String) -> String = { QueryHighlighter.highlight(it, query) }
  private val noHighlighter: (String) -> String = { it }

  init {
    component.add(searchListComponent)
    searchListComponent.selectionMode = ListSelectionModel.SINGLE_SELECTION
    searchListComponent.installCellRenderer { renderSearchListCell(it) }
  }

  private fun renderSearchListCell(targetNode: TargetNode.Target): JPanel {
    val renderedCell = JPanel(VerticalLayout(0))
    val chosenHighlighter: (String) -> String =
      if (searchListModel.size() <= MAX_TARGETS_TO_HIGHLIGHT) {
        queryHighlighter
      } else {
        noHighlighter
      }
    renderedCell.add(targetNode.asComponent(iconProvider, chosenHighlighter))
    return renderedCell
  }

  override fun rerender() {
    val nodes = targets.prepareTargetNodes()
    replaceSearchListElementsWith(nodes)
    maybeAddShowMoreButton(nodes)
  }

  private fun Tristate.Targets.prepareTargetNodes(): List<TargetNode.Target> =
    this.toTargetNodes()
      .reduce { a, b -> a + b }
      .sortedBy(TargetNode.Target::displayName)

  private fun Tristate.Targets.toTargetNodes(): Tristate<List<TargetNode.Target>> {
    val loadedNodes = loaded.map { it.toValidTargetNode(true) }
    val unloadedNodes = unloaded.map { it.toValidTargetNode(false) }
    val invalidNodes = invalid.map { it.toInvalidTargetNode() }
    return Tristate(loadedNodes, unloadedNodes, invalidNodes)
  }

  private fun BuildTargetInfo.toValidTargetNode(loaded: Boolean = true): TargetNode.Target =
    TargetNode.ValidTarget(this, this.id, loaded)

  private fun BuildTargetId.toInvalidTargetNode(): TargetNode.Target =
    TargetNode.InvalidTarget(this, this)

  private fun replaceSearchListElementsWith(printableTargets: Collection<TargetNode.Target>) {
    component.remove(showMoreButton)
    searchListModel.removeAllElements()
    searchListModel.addAll(printableTargets)
  }

  private fun maybeAddShowMoreButton(targets: Collection<TargetNode.Target>) {
    val remainingTargets = targets.size - MAX_TARGETS_TO_HIGHLIGHT
    if (remainingTargets > 0) {
      showMoreButton = JButton(BspPluginBundle.message("widget.show.more.targets.button", remainingTargets))
      showMoreButton.addActionListener {
        showMoreTargets(targets)
      }
      component.add(showMoreButton)
    }
  }

  private fun showMoreTargets(targets: Collection<TargetNode.Target>) {
    component.remove(showMoreButton)
    replaceSearchListElementsWith(targets)
  }

  override fun addMouseListener(mouseListener: MouseListener) {
    searchListComponent.addMouseListener(mouseListener)
  }

  override fun getSelectedNode(): TargetNode? =
    searchListComponent.selectedValue

  // Fixes https://youtrack.jetbrains.com/issue/BAZEL-522
  public fun selectAtLocation(location: Point) {
    val index = searchListComponent.locationToIndex(location)
    if (index >= 0 && !searchListComponent.isSelectedIndex(index)) {
      searchListComponent.selectedIndex = index
    }
  }
}
