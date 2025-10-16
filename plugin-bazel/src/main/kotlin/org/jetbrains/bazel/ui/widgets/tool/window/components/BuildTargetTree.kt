package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.TreeUIHelper
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.BuildTargetClassifierExtension
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.ListTargetClassifier
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.TreeTargetClassifier
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import org.jetbrains.bsp.protocol.BuildTarget
import java.awt.Point
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

internal class BuildTargetTree(
  private val project: Project,
  private val rootNode: DefaultMutableTreeNode = DefaultMutableTreeNode(DirectoryNodeData("[root]", emptyList())),
) : Tree(rootNode) {
  private val loadedTargetsMouseListener: LoadedTargetsMouseListener =
    object : LoadedTargetsMouseListener(project) {
      override fun getSelectedComponentName(): String {
        val selected = lastSelectedPathComponent as? DefaultMutableTreeNode
        return when (val userObject = selected?.userObject) {
          is TargetNodeData -> userObject.displayName
          is DirectoryNodeData -> userObject.name
          else -> ""
        }
      }

      override fun isPointSelectable(point: Point): Boolean = getPathForLocation(point.x, point.y) != null

      override fun getSelectedBuildTarget(): BuildTarget? {
        val selected = lastSelectedPathComponent as? DefaultMutableTreeNode
        val userObject = selected?.userObject
        return if (userObject is TargetNodeData) {
          project.targetUtils.getBuildTargetForLabel(userObject.id)
        } else {
          null
        }
      }

      override fun getSelectedBuildTargetsUnderDirectory(): List<BuildTarget> {
        val selected = lastSelectedPathComponent as? DefaultMutableTreeNode
        val userObject = selected?.userObject
        val targetUtils = project.targetUtils
        return (
          if (userObject is DirectoryNodeData) {
            userObject.targets.mapNotNull { targetUtils.getBuildTargetForLabel(it.id) }
          } else {
            emptyList()
          }
        )
      }

      override val copyTargetIdAction: CopyTargetIdAction =
        object : CopyTargetIdAction.FromContainer(this@BuildTargetTree) {
          override fun getTargetInfo(): BuildTarget? = getSelectedBuildTarget()
        }

      override val bazelJumpToBuildFileAction: BazelJumpToBuildFileAction =
        BazelJumpToBuildFileAction.NonXmlRegistered(::getSelectedBuildTarget).also {
          it.registerShortcut(this@BuildTargetTree)
        }
    }

  init {
    selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    cellRenderer = TargetTreeCellRenderer { it }
    isRootVisible = false

    // Initialize speed search
    TreeUIHelper.getInstance().installTreeSpeedSearch(this, {
      val lastPathComponent = (it.lastPathComponent as? DefaultMutableTreeNode)?.userObject ?: return@installTreeSpeedSearch null
      when (lastPathComponent) {
        is DirectoryNodeData -> lastPathComponent.name
        is TargetNodeData -> lastPathComponent.displayName
        else -> null
      }
    }, true)

    registerKeyboardShortcutForPopup(loadedTargetsMouseListener)
    addMouseListener(loadedTargetsMouseListener)
  }

  fun updateTree(visibleTargets: Collection<Label>, displayAsTree: Boolean) {
    val classifier =
      if (displayAsTree) {
        TreeTargetClassifier(project)
      } else {
        ListTargetClassifier(project)
      }

    // Generate the tree with the current targets
    generateTree(visibleTargets, classifier)

    // Notify the tree model that the structure has changed
    (treeModel as? DefaultTreeModel)?.reload()

    expandPath(TreePath(rootNode.path))
  }

  private fun generateTree(targets: Collection<Label>, classifier: BuildTargetClassifierExtension) {
    generateTreeFromIdentifiers(
      targets.map {
        BuildTargetTreeIdentifier(
          id = it,
          path = classifier.calculateBuildTargetPath(it),
          displayName = classifier.calculateBuildTargetName(it),
        )
      },
      classifier.separator,
    )
  }

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
      TargetNodeData(identifier.displayName, identifier.id),
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

  private fun registerKeyboardShortcutForPopup(popupHandler: PopupHandler) {
    val popupAction =
      SimpleAction {
        val selectionPath = selectionPath ?: return@SimpleAction
        val directoryIsSelected = (selectionPath.lastPathComponent as? DefaultMutableTreeNode)?.isLeaf == false
        if (directoryIsSelected) {
          // Normally, this is a built-in feature of a Tree.
          //   However, it stops working out-of-the-box since its keyboard shortcut has been overridden.
          toggleExpansion(selectionPath)
        } else {
          val selectedPoint = selectionPath.let { getPathBounds(it)?.location } ?: return@SimpleAction
          popupHandler.invokePopup(this, selectedPoint.x, selectedPoint.y)
        }
      }
    popupAction.registerCustomShortcutSet(BspShortcuts.openTargetContextMenu, this@BuildTargetTree)
  }

  private fun toggleExpansion(path: TreePath) {
    if (isCollapsed(path)) {
      expandPath(path)
    } else {
      collapsePath(path)
    }
  }
}

data class DirectoryNodeData(
  @JvmField val name: String,
  @JvmField val targets: List<BuildTargetTreeIdentifier>,
)

data class TargetNodeData(
  @JvmField val displayName: String,
  @JvmField val id: Label,
)

data class BuildTargetTreeIdentifier(
  @JvmField val id: Label,
  @JvmField val path: List<String>,
  @JvmField val displayName: String,
)
