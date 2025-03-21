package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.extensionPoints.BazelBuildTargetClassifier
import org.jetbrains.bazel.extensionPoints.DefaultBuildTargetClassifierExtension
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import java.awt.Component
import java.awt.Point
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

class BuildTargetTree(
  private val targetIcon: Icon,
  private val invalidTargetIcon: Icon,
  private val targets: Collection<BuildTargetInfo>,
  private val invalidTargets: List<Label>,
  private val labelHighlighter: (String) -> String = { it },
  private val showAsList: Boolean = false,
) : BuildTargetContainer {
  private val rootNode = DefaultMutableTreeNode(DirectoryNodeData("[root]", emptyList()))
  private val cellRenderer = TargetTreeCellRenderer(targetIcon, invalidTargetIcon, labelHighlighter)
  private var popupHandlerBuilder: ((BuildTargetContainer) -> PopupHandler)? = null

  val treeComponent: Tree = Tree(rootNode)

  private val bspBuildTargetClassifier =
    if (showAsList) {
      DefaultBuildTargetClassifierExtension
    } else {
      BazelBuildTargetClassifier
    }

  override val copyTargetIdAction: CopyTargetIdAction = CopyTargetIdAction.FromContainer(this, treeComponent)

  init {
    treeComponent.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    treeComponent.cellRenderer = cellRenderer
    treeComponent.isRootVisible = false
    generateTree()
    treeComponent.expandPath(TreePath(rootNode.path))
  }

  private fun generateTree() {
    generateTreeFromIdentifiers(
      targets.map {
        BuildTargetTreeIdentifier(
          it.id,
          it,
          bspBuildTargetClassifier.calculateBuildTargetPath(it),
          bspBuildTargetClassifier.calculateBuildTargetName(it),
        )
      } +
        invalidTargets.map {
          BuildTargetTreeIdentifier(
            it,
            null,
            bspBuildTargetClassifier.calculateBuildTargetPath(it.toFakeBuildTargetInfo()),
            bspBuildTargetClassifier.calculateBuildTargetName(it.toFakeBuildTargetInfo()),
          )
        },
      bspBuildTargetClassifier.separator,
    )
  }

  // only used with Bazel projects
  private fun Label.toFakeBuildTargetInfo() = BuildTargetInfo(id = this)

  private fun generateTreeFromIdentifiers(targets: List<BuildTargetTreeIdentifier>, separator: String?) {
    val pathToIdentifierMap = targets.groupBy { it.path.firstOrNull() }
    val sortedDirs =
      pathToIdentifierMap.keys
        .filterNotNull()
        .sorted()
    rootNode.removeAllChildren()
    for (dir in sortedDirs) {
      pathToIdentifierMap[dir]?.let {
        rootNode.add(generateDirectoryNode(it, dir, separator, 0))
      }
    }
    pathToIdentifierMap[null]?.forEach {
      rootNode.add(generateTargetNode(it))
    }
  }

  private fun generateDirectoryNode(
    targets: List<BuildTargetTreeIdentifier>,
    dirname: String,
    separator: String?,
    depth: Int,
  ): DefaultMutableTreeNode {
    val node = DefaultMutableTreeNode()
    node.userObject = DirectoryNodeData(dirname, targets)

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
    pathToIdentifierMap: Map<String?, List<BuildTargetTreeIdentifier>>,
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
    pathToIdentifierMap: Map<String?, List<BuildTargetTreeIdentifier>>,
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

  private fun generateChildrenTargetNodes(pathToIdentifierMap: Map<String?, List<BuildTargetTreeIdentifier>>): List<TreeNode> =
    pathToIdentifierMap[null]
      ?.sortedBy { it.displayName }
      ?.map { generateTargetNode(it) }
      ?: emptyList()

  private fun generateTargetNode(identifier: BuildTargetTreeIdentifier): DefaultMutableTreeNode =
    DefaultMutableTreeNode(
      TargetNodeData(identifier.id, identifier.target, identifier.displayName, identifier.target != null),
    )

  private fun simplifyNodeIfHasOneChild(
    node: DefaultMutableTreeNode,
    dirname: String,
    separator: String?,
    childrenList: MutableList<TreeNode>,
  ) {
    if (separator != null && childrenList.size == 1) {
      val onlyChild = childrenList.first() as? DefaultMutableTreeNode
      val onlyChildObject = onlyChild?.userObject
      if (onlyChildObject is DirectoryNodeData) {
        node.userObject = DirectoryNodeData("${dirname}${separator}${onlyChildObject.name}", onlyChildObject.targets)
        childrenList.clear()
        childrenList.addAll(onlyChild.children().toList())
      }
    }
  }

  override fun isEmpty(): Boolean = rootNode.isLeaf

  override fun getComponent(): JComponent = treeComponent

  override fun registerPopupHandler(popupHandlerBuilder: (BuildTargetContainer) -> PopupHandler) {
    this.popupHandlerBuilder = popupHandlerBuilder
    val newPopupHandler = popupHandlerBuilder(this).also { it.registerKeyboardShortcutForPopup() }
    treeComponent.addMouseListener(newPopupHandler)
  }

  private fun PopupHandler.registerKeyboardShortcutForPopup() {
    val popupAction =
      SimpleAction {
        val selectionPath = treeComponent.selectionPath ?: return@SimpleAction
        val directoryIsSelected = (selectionPath.lastPathComponent as? DefaultMutableTreeNode)?.isLeaf == false
        if (directoryIsSelected) {
          // Normally, this is a built-in feature of a Tree.
          //   However, it stops working out-of-the-box since its keyboard shortcut has been overridden.
          selectionPath.toggleExpansion()
        } else {
          val selectedPoint = selectionPath.let { treeComponent.getPathBounds(it)?.location } ?: return@SimpleAction
          this.invokePopup(treeComponent, selectedPoint.x, selectedPoint.y)
        }
      }
    popupAction.registerCustomShortcutSet(BspShortcuts.openTargetContextMenu, treeComponent)
  }

  private fun TreePath.toggleExpansion() {
    if (treeComponent.isCollapsed(this)) {
      treeComponent.expandPath(this)
    } else {
      treeComponent.collapsePath(this)
    }
  }

  override fun getSelectedBuildTarget(): BuildTargetInfo? {
    val selected = treeComponent.lastSelectedPathComponent as? DefaultMutableTreeNode
    val userObject = selected?.userObject
    return if (userObject is TargetNodeData) {
      userObject.target
    } else {
      null
    }
  }

  override fun getSelectedBuildTargetsUnderDirectory(): List<BuildTargetInfo> {
    val selected = treeComponent.lastSelectedPathComponent as? DefaultMutableTreeNode
    val userObject = selected?.userObject
    return (
      if (userObject is DirectoryNodeData) {
        userObject.targets.mapNotNull { it.target }
      } else {
        emptyList()
      }
    )
  }

  override fun getSelectedComponentName(): String {
    val selected = treeComponent.lastSelectedPathComponent as? DefaultMutableTreeNode
    val userObject = selected?.userObject
    return when (userObject) {
      is TargetNodeData -> userObject.displayName
      is DirectoryNodeData -> userObject.name
      else -> ""
    }
  }

  override fun selectTopTargetAndFocus() {
    treeComponent.selectionRows = intArrayOf(0)
    treeComponent.requestFocus()
  }

  override fun isPointSelectable(point: Point): Boolean = treeComponent.getPathForLocation(point.x, point.y) != null

  override fun createNewWithTargets(newTargets: Collection<BuildTargetInfo>, newInvalidTargets: List<Label>): BuildTargetTree =
    createNewWithTargetsAndHighlighter(newTargets, newInvalidTargets, labelHighlighter)

  fun createNewWithTargetsAndHighlighter(
    newTargets: Collection<BuildTargetInfo>,
    newInvalidTargets: List<Label>,
    labelHighlighter: (String) -> String,
  ): BuildTargetTree {
    val new =
      BuildTargetTree(
        targetIcon = targetIcon,
        invalidTargetIcon = invalidTargetIcon,
        targets = newTargets,
        invalidTargets = newInvalidTargets,
        labelHighlighter = labelHighlighter,
        showAsList = showAsList,
      )
    popupHandlerBuilder?.let { new.registerPopupHandler(it) }
    return new
  }
}

private interface NodeData

private data class DirectoryNodeData(val name: String, val targets: List<BuildTargetTreeIdentifier>) : NodeData {
  override fun toString(): String = name
}

private data class TargetNodeData(
  val id: Label,
  val target: BuildTargetInfo?,
  val displayName: String,
  val isValid: Boolean,
) : NodeData {
  override fun toString(): String = target?.displayName ?: id.toShortString()
}

private data class BuildTargetTreeIdentifier(
  val id: Label,
  val target: BuildTargetInfo?,
  val path: List<String>,
  val displayName: String,
)

private class TargetTreeCellRenderer(
  val targetIcon: Icon,
  val invalidTargetIcon: Icon,
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
  ): Component =
    when (val userObject = (value as? DefaultMutableTreeNode)?.userObject) {
      is DirectoryNodeData ->
        JBLabel(
          labelHighlighter(userObject.name),
          PlatformIcons.FOLDER_ICON,
          SwingConstants.LEFT,
        )

      is TargetNodeData ->
        if (userObject.isValid) {
          JBLabel(
            labelHighlighter(userObject.displayName),
            targetIcon,
            SwingConstants.LEFT,
          )
        } else {
          JBLabel(
            labelStrikethrough(userObject.displayName),
            invalidTargetIcon,
            SwingConstants.LEFT,
          )
        }

      else ->
        JBLabel(
          BazelPluginBundle.message("widget.no.renderable.component"),
          PlatformIcons.ERROR_INTRODUCTION_ICON,
          SwingConstants.LEFT,
        )
    }

  private fun labelStrikethrough(text: String): String = "<html><s>$text</s><html>"
}
