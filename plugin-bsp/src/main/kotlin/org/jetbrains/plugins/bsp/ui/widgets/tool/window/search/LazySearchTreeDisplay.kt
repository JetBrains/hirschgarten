package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import ch.epfl.scala.bsp4j.BuildTarget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetTree
import java.awt.event.MouseListener
import javax.swing.Icon

public class LazySearchTreeDisplay(icon: Icon, toolName: String) : LazySearchDisplay() {
  private var targetTree: BuildTargetTree = BuildTargetTree(icon, toolName, targets)

  override fun rerender() {
    component.remove(targetTree.treeComponent)
    targetTree = targetTree.createNewWithTargetsAndHighlighter(targets) { QueryHighlighter.highlight(it, query) }
    component.add(targetTree.treeComponent)
  }

  override fun addMouseListener(mouseListener: MouseListener) {
    targetTree.addMouseListener { _ -> mouseListener }
  }

  override fun getSelectedBuildTarget(): BuildTarget? =
    targetTree.getSelectedBuildTarget()
}
