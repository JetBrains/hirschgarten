package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetTree
import java.awt.event.MouseListener
import javax.swing.Icon

public class LazySearchTreeDisplay(icon: Icon, buildToolId: BuildToolId) : LazySearchDisplay() {
  private var targetTree: BuildTargetTree = BuildTargetTree(icon, icon, buildToolId, targets, emptyList())

  override fun rerender() {
    component.remove(targetTree.treeComponent)
    targetTree = targetTree.createNewWithTargetsAndHighlighter(targets) { QueryHighlighter.highlight(it, query) }
    component.add(targetTree.treeComponent)
  }

  override fun addMouseListener(mouseListener: MouseListener) {
    targetTree.addMouseListener { _ -> mouseListener }
  }

  override fun getSelectedBuildTarget(): BuildTargetInfo? =
    targetTree.getSelectedBuildTarget()
}
