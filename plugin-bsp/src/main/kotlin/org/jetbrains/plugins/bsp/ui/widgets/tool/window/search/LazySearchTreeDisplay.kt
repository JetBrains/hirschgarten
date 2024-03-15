package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetTree
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TargetNode
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate
import java.awt.event.MouseListener
import javax.swing.Icon

public class LazySearchTreeDisplay(
  iconProvider: Tristate<Icon>,
  buildToolId: BuildToolId,
) : LazySearchDisplay() {
  private var targetTree: BuildTargetTree = BuildTargetTree(iconProvider, buildToolId, targets)

  override fun rerender() {
    component.remove(targetTree.treeComponent)
    targetTree = targetTree.createNewWithTargetsAndHighlighter(targets) { QueryHighlighter.highlight(it, query) }
    component.add(targetTree.treeComponent)
  }

  override fun addMouseListener(mouseListener: MouseListener) {
    targetTree.addMouseListener { _ -> mouseListener }
  }

  override fun getSelectedNode(): TargetNode? =
    targetTree.getSelectedNode()
}
