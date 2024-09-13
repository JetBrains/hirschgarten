package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetTree
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import java.awt.event.MouseListener
import javax.swing.Icon

public class LazySearchListDisplay(icon: Icon, buildToolId: BuildToolId) : LazySearchDisplay() {
  private var targetTree: BuildTargetTree = BuildTargetTree(icon, icon, buildToolId, targets, emptyList())
  private var listBuildTargetClassifier = BuildTargetClassifierExtension.ep.withBuildToolId(bspBuildToolId)

  override fun rerender() {
    component.remove(targetTree.treeComponent)
    targetTree = targetTree.createNewWithTargetsAndHighlighter(targets, listBuildTargetClassifier) { QueryHighlighter.highlight(it, query) }
    component.add(targetTree.treeComponent)
  }

  override fun addMouseListener(mouseListener: MouseListener) {
    targetTree.addMouseListener { _ -> mouseListener }
  }

  override fun getSelectedBuildTarget(): BuildTargetInfo? = targetTree.getSelectedBuildTarget()
}
