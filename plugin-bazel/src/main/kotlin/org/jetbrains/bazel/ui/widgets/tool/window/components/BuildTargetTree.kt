package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.BazelBuildTargetClassifier
import org.jetbrains.bazel.extensionPoints.buildTargetClassifier.DefaultBuildTargetClassifierExtension
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bazel.ui.widgets.tool.window.model.BuildTargetsModel
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import org.jetbrains.bsp.protocol.BuildTarget
import java.awt.Component
import java.awt.Point
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class BuildTargetTree(
  private val project: Project,
  private val model: BuildTargetsModel,
  private val rootNode: DefaultMutableTreeNode = DefaultMutableTreeNode(DirectoryNodeData("[root]", emptyList())),
) : Tree(rootNode) {

  private val loadedTargetsMouseListener: LoadedTargetsMouseListener = object : LoadedTargetsMouseListener(project) {
    override fun getSelectedComponentName(): String {
      val selected = lastSelectedPathComponent as? DefaultMutableTreeNode
      val userObject = selected?.userObject
      return when (userObject) {
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
        userObject.target
      } else {
        null
      }
    }

    override fun getSelectedBuildTargetsUnderDirectory(): List<BuildTarget> {
      val selected = lastSelectedPathComponent as? DefaultMutableTreeNode
      val userObject = selected?.userObject
      return (
        if (userObject is DirectoryNodeData) {
          userObject.targets.mapNotNull { it.target }
        } else {
          emptyList()
        }
      )
    }

    override val copyTargetIdAction: CopyTargetIdAction = object : CopyTargetIdAction.FromContainer(this@BuildTargetTree) {
      override fun getTargetInfo(): BuildTarget? = getSelectedBuildTarget()
    }


  }.also {
    it.registerKeyboardShortcutForPopup()
    addMouseListener(it)
  }



  private var bspBuildTargetClassifier =
    if (model.displayAsTree) {
      BazelBuildTargetClassifier(project)
    } else {
      DefaultBuildTargetClassifierExtension(project)
    }



  init {
    selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    cellRenderer = TargetTreeCellRenderer { it }
    isRootVisible = false

    // Listen for model changes
    model.addListener {
      updateTree()
    }

    // Initial tree generation
    updateTree()
    expandPath(TreePath(rootNode.path))
  }

  private fun updateTree() {
    // Update the classifier if display mode changed
      bspBuildTargetClassifier = if (model.displayAsTree) {
        BazelBuildTargetClassifier(project)
      } else {
        DefaultBuildTargetClassifierExtension(project)
      }

    // Generate the tree with the current targets
    val targetsToDisplay = if (model.isSearchActive) model.searchResults else model.targets
    generateTree(targetsToDisplay, model.invalidTargets)
    expandPath(TreePath(rootNode.path))
  }

  private fun generateTree(targets: Collection<BuildTarget>, invalidTargets: List<Label>) {
    generateTreeFromIdentifiers(
      targets.map {
        BuildTargetTreeIdentifier(
          it.id,
          it,
          bspBuildTargetClassifier.calculateBuildTargetPath(it.id),
          bspBuildTargetClassifier.calculateBuildTargetName(it.id),
        )
      } +
        invalidTargets.map {
          BuildTargetTreeIdentifier(
            it,
            null,
            bspBuildTargetClassifier.calculateBuildTargetPath(it),
            bspBuildTargetClassifier.calculateBuildTargetName(it),
          )
        },
      bspBuildTargetClassifier.separator,
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
      TargetNodeData(project, identifier.id, identifier.target, identifier.displayName, identifier.target != null),
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

  private fun PopupHandler.registerKeyboardShortcutForPopup() {
    val popupAction =
      SimpleAction {
        val selectionPath = selectionPath ?: return@SimpleAction
        val directoryIsSelected = (selectionPath.lastPathComponent as? DefaultMutableTreeNode)?.isLeaf == false
        if (directoryIsSelected) {
          // Normally, this is a built-in feature of a Tree.
          //   However, it stops working out-of-the-box since its keyboard shortcut has been overridden.
          selectionPath.toggleExpansion()
        } else {
          val selectedPoint = selectionPath.let { getPathBounds(it)?.location } ?: return@SimpleAction
          this.invokePopup(this, selectedPoint.x, selectedPoint.y)
        }
      }
    popupAction.registerCustomShortcutSet(BspShortcuts.openTargetContextMenu, this@BuildTargetTree)
  }

  private fun TreePath.toggleExpansion() {
    if (isCollapsed(this)) {
      expandPath(this)
    } else {
      collapsePath(this)
    }
  }



  fun selectTopTargetAndFocus() {
    selectionRows = intArrayOf(0)
    requestFocus()
  }

}

private interface NodeData

private data class DirectoryNodeData(val name: String, val targets: List<BuildTargetTreeIdentifier>) : NodeData {
  override fun toString(): String = name
}

private data class TargetNodeData(
  val project: Project,
  val id: Label,
  val target: BuildTarget?,
  val displayName: String,
  val isValid: Boolean,
) : NodeData {
  override fun toString(): String = id.toShortString(project)
}

private data class BuildTargetTreeIdentifier(
  val id: Label,
  val target: BuildTarget?,
  val path: List<String>,
  val displayName: String,
)

private class TargetTreeCellRenderer(
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
            BazelPluginIcons.bazel,
            SwingConstants.LEFT,
          )
        } else {
          JBLabel(
            labelStrikethrough(userObject.displayName),
            BazelPluginIcons.bazelError,
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


private object QueryHighlighter {
  fun highlight(text: String, query: Regex): String {
    val matches = query.findAll(text).take(text.length).toList()
    return if (query.pattern.isNotEmpty() && matches.isNotEmpty()) {
      "<html>${highlightAllOccurrences(text, matches)}</html>"
    } else {
      text
    }
  }

  private fun highlightAllOccurrences(text: String, query: List<MatchResult>): String {
    val result = StringBuilder()
    var lastIndex = 0
    for (match in query) {
      val matchStart = match.range.first
      val matchEnd = match.range.last + 1
      result.append(text, lastIndex, matchStart)
      result.append("<b><u>")
      result.append(text, matchStart, matchEnd)
      result.append("</u></b>")
      lastIndex = matchEnd
    }
    result.append(text.substring(lastIndex))
    return result.toString()
  }
}
