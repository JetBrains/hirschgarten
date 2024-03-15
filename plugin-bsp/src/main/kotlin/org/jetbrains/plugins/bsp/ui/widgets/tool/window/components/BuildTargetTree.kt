package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.extension.points.BuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TargetNode
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate
import java.awt.Component
import java.awt.event.MouseListener
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

public class BuildTargetTree(
  private val iconProvider: Tristate<Icon>,
  private val buildToolId: BuildToolId,
  private val targetProvider: Tristate.Targets,
  private val labelHighlighter: (String) -> String = { it },
) : BuildTargetContainer {
  private val rootNode = DefaultMutableTreeNode(TargetNode.Directory("[root]"))
  private val cellRenderer = TargetTreeCellRenderer(iconProvider, labelHighlighter)
  private val mouseListenerBuilders = mutableSetOf<(BuildTargetContainer) -> MouseListener>()

  public val treeComponent: Tree = Tree(rootNode)

  override val copyTargetIdAction: CopyTargetIdAction = CopyTargetIdAction(this, treeComponent)

  init {
    treeComponent.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    treeComponent.cellRenderer = cellRenderer
    treeComponent.isRootVisible = false
    generateTree()
    treeComponent.expandPath(TreePath(rootNode.path))
  }

  private fun generateTree() {
    val bspBuildTargetClassifier =
      BuildTargetClassifierExtension.ep.withBuildToolIdOrDefault(buildToolId)

    val loaded =
      targetProvider.loaded.map { it.toNodeWithPath(bspBuildTargetClassifier) }
    val unloaded =
      targetProvider.unloaded.map { it.toNodeWithPath(bspBuildTargetClassifier, loaded = false) }
    val invalid =
      targetProvider.invalid.map { it.toInvalidNodeWithPath(bspBuildTargetClassifier) }

    generateTreeFromIdentifiers(
      targets = loaded + unloaded + invalid,
      separator = bspBuildTargetClassifier.separator,
    )
  }

  private fun BuildTargetInfo.toNodeWithPath(
    classifier: BuildTargetClassifierExtension,
    invalid: Boolean = false,
    loaded: Boolean = true,
  ): TargetNodeWithPath {
    val path = classifier.calculateBuildTargetPath(this)
    val displayName = classifier.calculateBuildTargetName(this)
    val targetNode = when {
      invalid -> TargetNode.InvalidTarget(id, displayName)
      else -> TargetNode.ValidTarget(this, displayName, loaded)
    }
    return TargetNodeWithPath(path, targetNode)
  }

  private fun BuildTargetId.toInvalidNodeWithPath(
    classifier: BuildTargetClassifierExtension,
  ): TargetNodeWithPath =
    BuildTargetInfo(id = this).toNodeWithPath(classifier = classifier, invalid = true)

  private fun generateTreeFromIdentifiers(targets: List<TargetNodeWithPath>, separator: String?) {
    val pathToIdentifierMap = targets.groupBy { it.path.firstOrNull() }
    val sortedDirs = pathToIdentifierMap.keys
      .filterNotNull()
      .sorted()
    rootNode.removeAllChildren()
    for (dir in sortedDirs) {
      pathToIdentifierMap[dir]?.let {
        rootNode.add(generateDirectoryNode(it, dir, separator, 0))
      }
    }
    pathToIdentifierMap[null]?.forEach {
      rootNode.add(it.targetNode.asMutableTreeNode())
    }
  }

  private fun generateDirectoryNode(
    targets: List<TargetNodeWithPath>,
    dirname: String,
    separator: String?,
    depth: Int,
  ): DefaultMutableTreeNode {
    val node = DefaultMutableTreeNode()
    node.userObject = TargetNode.Directory(dirname)

    val pathToIdentifierMap = targets.groupBy { it.path.getOrNull(depth + 1) }
    val childrenNodeList =
      generateChildrenNodes(pathToIdentifierMap, separator, depth).toMutableList()

    simplifyNodeIfHasOneChild(node, dirname, separator, childrenNodeList)
    childrenNodeList.forEachIndexed { index, child ->
      if (child is MutableTreeNode) {
        node.insert(child, index)
      }
    }
    return node
  }

  private fun generateChildrenNodes(
    pathToIdentifierMap: Map<String?, List<TargetNodeWithPath>>,
    separator: String?,
    depth: Int,
  ): List<TreeNode> {
    val childrenDirectoryNodes =
      generateChildrenDirectoryNodes(pathToIdentifierMap, separator, depth)
    val childrenTargetNodes =
      generateChildrenTargetNodes(pathToIdentifierMap)
    return childrenDirectoryNodes + childrenTargetNodes
  }

  private fun generateChildrenDirectoryNodes(
    pathToIdentifierMap: Map<String?, List<TargetNodeWithPath>>,
    separator: String?,
    depth: Int,
  ): List<TreeNode> =
    pathToIdentifierMap
      .keys
      .filterNotNull()
      .sorted()
      .mapNotNull { dir ->
        pathToIdentifierMap[dir]?.let {
          generateDirectoryNode(it, dir, separator, depth + 1)
        }
      }

  private fun generateChildrenTargetNodes(
    pathToIdentifierMap: Map<String?, List<TargetNodeWithPath>>,
  ): List<TreeNode> =
    pathToIdentifierMap[null]?.map { it.targetNode.asMutableTreeNode() } ?: emptyList()

  private fun simplifyNodeIfHasOneChild(
    node: DefaultMutableTreeNode,
    dirname: String,
    separator: String?,
    childrenList: MutableList<TreeNode>,
  ) {
    if (separator != null && childrenList.size == 1) {
      val onlyChild = childrenList.first() as? DefaultMutableTreeNode
      val onlyChildObject = onlyChild?.userObject
      if (onlyChildObject is TargetNode.Directory) {
        node.userObject = TargetNode.Directory("${dirname}${separator}${onlyChildObject.displayName}")
        childrenList.clear()
        childrenList.addAll(onlyChild.children().toList())
      }
    }
  }

  override fun isEmpty(): Boolean = rootNode.isLeaf

  override fun addMouseListener(listenerBuilder: (BuildTargetContainer) -> MouseListener) {
    mouseListenerBuilders.add(listenerBuilder)
    treeComponent.addMouseListener(listenerBuilder(this))
  }

  override fun getSelectedNode(): TargetNode? {
    val selected = treeComponent.lastSelectedPathComponent as? DefaultMutableTreeNode
    return selected?.userObject as? TargetNode
  }

  override fun createNewWithTargets(newTargets: Tristate.Targets): BuildTargetTree =
    createNewWithTargetsAndHighlighter(newTargets, labelHighlighter)

  public fun createNewWithTargetsAndHighlighter(
    newTargets: Tristate.Targets,
    labelHighlighter: (String) -> String,
  ): BuildTargetTree {
    val new = BuildTargetTree(iconProvider, buildToolId, newTargets, labelHighlighter)
    for (listenerBuilder in this.mouseListenerBuilders) {
      new.addMouseListener(listenerBuilder)
    }
    return new
  }

  override fun getComponent(): JComponent = treeComponent
}

private data class TargetNodeWithPath(
  val path: List<String>,
  val targetNode: TargetNode.Target,
)

private class TargetTreeCellRenderer(
  val iconProvider: Tristate<Icon>,
  val labelHighlighter: (String) -> String,
) : TreeCellRenderer {
  override fun getTreeCellRendererComponent(
    tree: JTree?,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    return when (val userObject = (value as? DefaultMutableTreeNode)?.userObject) {
      is TargetNode -> userObject.asComponent(iconProvider, labelHighlighter)

      else -> JBLabel(
        BspPluginBundle.message("widget.no.renderable.component"),
        PlatformIcons.ERROR_INTRODUCTION_ICON,
        SwingConstants.LEFT,
      )
    }
  }
}
