package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import org.jetbrains.plugins.bsp.extension.points.BspBuildTargetClassifierExtension
import java.awt.Component
import javax.swing.Icon
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

private interface NodeData
private data class DirectoryNodeData(val name: String) : NodeData
private data class TargetNodeData(val target: BuildTarget, val displayName: String) : NodeData

private data class BuildTargetTreeIdentifier(
  val target: BuildTarget,
  val path: List<String>,
  val displayName: String
)

private class TargetTreeCellRenderer(val targetIcon: Icon) : TreeCellRenderer {
  override fun getTreeCellRendererComponent(
    tree: JTree?,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    return when (val userObject = (value as? DefaultMutableTreeNode)?.userObject) {
      is DirectoryNodeData -> JBLabel(
        userObject.name,
        PlatformIcons.FOLDER_ICON,
        SwingConstants.LEFT
      )

      is TargetNodeData -> JBLabel(
        userObject.displayName,
        targetIcon,
        SwingConstants.LEFT
      )

      else -> JBLabel(
        "[not renderable]",
        PlatformIcons.ERROR_INTRODUCTION_ICON,
        SwingConstants.LEFT
      )  // TODO - temporary
    }
  }
}

/**
 * Responsible for generating a build target tree
 * @param targetIcon an icon for all build targets in the generated tree
 */
public class BspTargetTree(targetIcon: Icon) {
  private val rootNode = DefaultMutableTreeNode(DirectoryNodeData("[root]"))

  public val treeComponent: Tree = Tree(rootNode)

  private val cellRenderer = TargetTreeCellRenderer(targetIcon)

  init {
    treeComponent.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    treeComponent.cellRenderer = cellRenderer
    treeComponent.isRootVisible = false
  }

  /**
   * Generates a tree of given build targets. The new tree replaces the previous one
   * @param toolName name of the BspBuildTargetClassifierExtension used to assemble the tree. If null, all
   * targets will be shown as a simple list
   * @param targets build targets to be put into the tree
   */
  public fun generateTree(toolName: String?, targets: Collection<BuildTarget>) {
    val bspBuildTargetClassifierProvider =
      BspBuildTargetClassifierProvider(toolName, BspBuildTargetClassifierExtension.extensions())
    generateTreeFromIdentifiers(
      targets.map {
        BuildTargetTreeIdentifier(
          it,
          bspBuildTargetClassifierProvider.getBuildTargetPath(it),
          bspBuildTargetClassifierProvider.getBuildTargetName(it)
        )
      },
      bspBuildTargetClassifierProvider.getSeparator()
    )
  }

  /**
   * Generates a tree using a list of BuildTargetTreeIdentifiers. The new tree replaces the previous one
   * @param targets a list of BuildTargetTreeIdentifiers to be put into the tree
   * @param separator separator to be used to chain directories
   * @see BspBuildTargetClassifier.separator
   */
  private fun generateTreeFromIdentifiers(targets: List<BuildTargetTreeIdentifier>, separator: String?) {
    val grouped: Map<String?, List<BuildTargetTreeIdentifier>> = targets.groupBy { it.path.firstOrNull() }
    val sortedDirs: List<String> = grouped
      .keys
      .filterNotNull()
      .sorted()
    rootNode.removeAllChildren()
    for (dir in sortedDirs) {
      rootNode.add(generateDirectoryNode(grouped[dir]!!, dir, separator, 0))
    }
    if (grouped.containsKey(null)) {
      for (identifier in grouped[null]!!) {
        rootNode.add(generateTargetNode(identifier))
      }
    }

    // Without the following line, target tree might not show (unexpanded root's children are invisible)
    treeComponent.expandPath(TreePath(rootNode.path))
  }

  /**
   * Recursively generates a directory node for the tree
   * @param targets a list of BuildTargetTreeIdentifiers to be put in this directory
   * @param dirname a name for this directory
   * @param separator separator to be used to chain directories
   * @param depth current recurrence depth
   * @return the created directory node
   * @see BspBuildTargetClassifier.separator
   */
  private fun generateDirectoryNode(
    targets: List<BuildTargetTreeIdentifier>,
    dirname: String,
    separator: String?,
    depth: Int
  ): DefaultMutableTreeNode {
    val node = DefaultMutableTreeNode()
    val childrenList: MutableList<TreeNode> = mutableListOf()

    node.userObject = DirectoryNodeData(dirname)
    val grouped: Map<String?, List<BuildTargetTreeIdentifier>> = targets.groupBy { it.path.getOrNull(depth + 1) }
    val sortedDirs: List<String> = grouped
      .keys
      .filterNotNull()
      .sorted()
    var index = 0
    for (dir in sortedDirs) {
      childrenList.add(generateDirectoryNode(grouped[dir]!!, dir, separator, depth + 1))
    }
    if (grouped.containsKey(null)) {
      for (identifier in grouped[null]!!) {
        childrenList.add(generateTargetNode(identifier))
      }
    }
    if (separator != null && childrenList.size == 1) {
      val child = childrenList.first() as? DefaultMutableTreeNode
      val childObject = child?.userObject
      if (childObject is DirectoryNodeData) {
        node.userObject = DirectoryNodeData("${dirname}${separator}${childObject.name}")
        childrenList.clear()
        childrenList.addAll(child.children().toList())
      }
    }
    for (child in childrenList) {
      if (child is MutableTreeNode) {
        node.insert(child, index++)
      }
    }
    return node
  }

  /**
   * Generates a node for a build target
   * @param identifier build target this node will represent
   * @return the created node
   */
  private fun generateTargetNode(identifier: BuildTargetTreeIdentifier): DefaultMutableTreeNode {
    return DefaultMutableTreeNode(TargetNodeData(identifier.target, identifier.displayName))
  }

  public companion object {
    /**
     * @return build target represented by the node selected in the tree. Is null when nothing is selected
     */
    public fun getSelectedBspTarget(tree: Tree): BuildTarget? {
      val selected: DefaultMutableTreeNode? = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
      val userObject: Any? = selected?.userObject
      if (userObject is TargetNodeData) {
        return userObject.target
      }
      return null
    }
  }
}