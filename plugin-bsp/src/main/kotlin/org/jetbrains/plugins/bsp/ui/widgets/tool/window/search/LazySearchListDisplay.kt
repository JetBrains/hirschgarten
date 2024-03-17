package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.VerticalLayout
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import java.awt.Point
import java.awt.event.MouseListener
import javax.swing.DefaultListModel
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

private const val TARGETS_TO_HIGHLIGHT: Int = 50

public class LazySearchListDisplay(private val icon: Icon) : LazySearchDisplay() {
  private val searchListModel = DefaultListModel<PrintableBuildTarget>()
  private val searchListComponent = JBList(searchListModel)
  private var showMoreButton = JButton("")

  init {
    component.add(searchListComponent)
    searchListComponent.selectionMode = ListSelectionModel.SINGLE_SELECTION
    searchListComponent.installCellRenderer { renderSearchListCell(it) }
  }

  private fun renderSearchListCell(printableBuildTarget: PrintableBuildTarget): JPanel {
    val renderedCell = JPanel(VerticalLayout(0))
    renderedCell.add(
      JBLabel(
        printableBuildTarget.displayName,
        icon,
        SwingConstants.LEFT,
      ),
    )
    return renderedCell
  }

  override fun rerender() {
    replaceSearchListElementsWith(takeSomeTargetsAndHighlight(targets))
    maybeAddShowMoreButton(targets)
  }

  private fun replaceSearchListElementsWith(printableTargets: Collection<PrintableBuildTarget>) {
    component.remove(showMoreButton)
    searchListModel.removeAllElements()
    searchListModel.addAll(printableTargets)
  }

  private fun takeSomeTargetsAndHighlight(targets: Collection<BuildTargetInfo>): List<PrintableBuildTarget> =
    targets.take(TARGETS_TO_HIGHLIGHT).map {
      PrintableBuildTarget(
        it,
        QueryHighlighter.highlight(it.getBuildTargetName(), query),
      )
    }

  private fun BuildTargetInfo.getBuildTargetName(): String =
    this.displayName ?: this.id

  private fun maybeAddShowMoreButton(targets: Collection<BuildTargetInfo>) {
    val remainingTargets = targets.size - TARGETS_TO_HIGHLIGHT
    if (remainingTargets > 0) {
      showMoreButton = JButton(BspPluginBundle.message("widget.show.more.targets.button", remainingTargets))
      showMoreButton.addActionListener {
        showMoreTargets(targets)
      }
      component.add(showMoreButton)
    }
  }

  private fun showMoreTargets(targets: Collection<BuildTargetInfo>) {
    component.remove(showMoreButton)
    replaceSearchListElementsWith(targets.map { PrintableBuildTarget(it) })
  }

  override fun addMouseListener(mouseListener: MouseListener) {
    searchListComponent.addMouseListener(mouseListener)
  }

  override fun getSelectedBuildTarget(): BuildTargetInfo? =
    searchListComponent.selectedValue?.buildTarget

  // https://youtrack.jetbrains.com/issue/BAZEL-522
  public fun selectAtLocation(location: Point) {
    val index = searchListComponent.locationToIndex(location)
    if (index >= 0 && !searchListComponent.isSelectedIndex(index)) {
      searchListComponent.selectedIndex = index
    }
  }
}
