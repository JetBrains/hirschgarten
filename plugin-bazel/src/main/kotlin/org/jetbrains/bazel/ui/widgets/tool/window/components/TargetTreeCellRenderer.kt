package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.components.JBLabel
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import java.awt.Color
import java.awt.Component
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellRenderer

class TargetTreeCellRenderer(val labelHighlighter: (String) -> String) : TreeCellRenderer {
  override fun getTreeCellRendererComponent(
    tree: JTree?,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component =
    when (val userObject = (value as? DefaultMutableTreeNode)?.userObject) {
      is DirectoryNodeData ->
        JBHiddenLabel(
          labelHighlighter(userObject.name),
          PlatformIcons.FOLDER_ICON,
          SwingConstants.LEFT,
          userObject.isHidden,
        )

      is TargetNodeData ->
        JBHiddenLabel(
          labelHighlighter(userObject.displayName),
          BazelPluginIcons.bazel,
          SwingConstants.LEFT,
          userObject.isHidden,
        )

      else ->
        JBLabel(
          BazelPluginBundle.message("widget.no.renderable.component"),
          PlatformIcons.ERROR_INTRODUCTION_ICON,
          SwingConstants.LEFT,
        )
    }
}

class JBHiddenLabel(
  @NlsContexts.Label text: String,
  icon: Icon,
  horizontalAlignment: Int,
  val isHidden: Boolean
) : JBLabel(text, icon, horizontalAlignment) {

  // more performant way
  override fun getForeground(): Color? {
    if (isHidden) {
      return super.getForeground().darker()
    }
    return super.getForeground()
  }
}
