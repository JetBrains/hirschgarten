package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import org.jetbrains.plugins.bsp.extension.points.BspBuildTargetClassifierExtension
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import java.awt.Component
import java.awt.event.MouseListener
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

private interface NodeData
private data class DirectoryNodeData(val name: String) : NodeData {
  override fun toString(): String = name
}
private data class TargetNodeData(val target: BuildTarget, val displayName: String) : NodeData {
  override fun toString(): String = target.displayName ?: target.id.uri
}

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
      )
    }
  }
}

/**
 * Responsible for generating a build target tree
 * @param targetIcon an icon for all build targets in the generated tree
 */
public class BspTargetTree(targetIcon: Icon) {
  public var panelComponent: JPanel = JPanel(VerticalLayout(0))
    private set

  private val targetSearch = TargetSearch(targetIcon, this::updateSearch)

  private val rootNode = DefaultMutableTreeNode(DirectoryNodeData("[root]"))
  private var treeComponent: Tree = Tree(rootNode)
  private val emptyTreeMessage = JBLabel(
    BspAllTargetsWidgetBundle.message("widget.no.targets.message"),
    SwingConstants.CENTER
  )

  private val cellRenderer = TargetTreeCellRenderer(targetIcon)

  private val mouseListenerBuilders = mutableSetOf<(JComponent) -> MouseListener>()

  init {
    treeComponent.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    treeComponent.cellRenderer = cellRenderer
    treeComponent.isRootVisible = false

    panelComponent.add(targetSearch.searchBarComponent, 0)
    panelComponent.add(treeComponent, 1)
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
    this.targetSearch.targets = targets
      .map { it.target }
      .sortedBy { it.displayName ?: it.id.uri }
    val grouped: Map<String?, List<BuildTargetTreeIdentifier>> = targets.groupBy { it.path.firstOrNull() }
    val sortedDirs: List<String> = grouped
      .keys
      .filterNotNull()
      .sorted()
    rootNode.removeAllChildren()
    for (dir in sortedDirs) {
      grouped[dir]?.let {
        rootNode.add(generateDirectoryNode(it, dir, separator, 0))
      }
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
      grouped[dir]?.let {
        childrenList.add(generateDirectoryNode(it, dir, separator, depth + 1))
      }
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

  /**
   * Replaces currently used `panelComponent` and `treeComponent` with new ones.
   * All mouse listeners added before are also added to the new tree.
   *
   * The newly created tree will have exact same structure as the previous one, unless this method is called
   * after `generateTree(...)`
   *
   * Previous panel will not be re-rendered automatically in its usage places - it has to be replaced with
   * its new instance, acquired from `panelComponent` after calling this method
   */
  public fun regenerateComponents() {
    val newPanel = JPanel(VerticalLayout(0))
    val newTree = Tree(rootNode)

    newTree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    newTree.cellRenderer = cellRenderer
    newTree.isRootVisible = false

    newTree.expandPath(TreePath(rootNode.path))

    treeComponent.getExpandedDescendants(TreePath(rootNode.path))?.let { expandedDescendants ->
      for (treePath in expandedDescendants) {
        newTree.expandPath(treePath)
      }
    }
    for (listenerBuilder in mouseListenerBuilders) {
      newTree.addMouseListener(
        listenerBuilder(newTree)
      )
    }

    treeComponent = newTree

    val treeIsEmpty = rootNode.isLeaf
    newPanel.add(targetSearch.searchBarComponent.also { it.isEnabled = !treeIsEmpty })
    newPanel.add(
      if (treeIsEmpty) emptyTreeMessage
      else treeComponent
    )

    panelComponent = newPanel
  }

  /**
   * Shows build targets list/tree, depending on current search query. This method is executed each time
   * the search query is modified.
   */
  private fun updateSearch() {
    val newPanelContent = chooseNewPanelContent()
    val oldPanelContent = getCurrentPanelContent()

    if (newPanelContent != oldPanelContent) {
      oldPanelContent?.let { panelComponent.remove(it) }
      panelComponent.add(newPanelContent)
      panelComponent.revalidate()
      panelComponent.repaint()
    }
  }

  private fun chooseNewPanelContent(): JComponent =
    when {
      rootNode.isLeaf -> emptyTreeMessage
      targetSearch.isSearchActive -> targetSearch.targetSearchPanel
      else -> treeComponent
    }

  private fun getCurrentPanelContent(): Component? =
    try {
      panelComponent.getComponent(1)
    } catch (_: ArrayIndexOutOfBoundsException) {
      null
    }

  /**
   * Adds mouse listeners to those components which display build targets
   * @param listenerBuilder a function which takes a component the listener will be put onto,
   * and returns a listener
   */
  public fun addMouseListeners(listenerBuilder: (JComponent) -> MouseListener) {
    this.mouseListenerBuilders.add(listenerBuilder)
    this.treeComponent.addMouseListener(
      listenerBuilder(this.treeComponent)
    )
    this.targetSearch.searchListComponent.addMouseListener(
      listenerBuilder(this.targetSearch.searchListComponent)
    )
  }

  public companion object {
    /**
     * @return build target represented by the selection in a list or a tree.
     * Is null when nothing is selected, or the component is nether a `JBList` nor a `Tree`
     * @param component the component to be checked
     */
    public fun getSelectedBspTarget(component: JComponent): BuildTarget? {
      return when (component) {
        is Tree -> getSelectedBspTargetFrom(component)
        is JBList<*> -> getSelectedBspTargetFrom(component)
        else -> null
      }
    }

    /**
     * @return build target represented by the selection in a list. Is null when nothing is selected
     * @param list the list to be checked
     */
    private fun <T> getSelectedBspTargetFrom(list: JBList<T>): BuildTarget? {
      return when (val sel: T = list.selectedValue) {
        is BuildTarget? -> sel
        is TargetSearch.PrintableBuildTarget -> sel.buildTarget
        else -> null
      }
    }

    /**
     * @return build target represented by the node selected in a tree. Is null when nothing is selected
     * @param tree the tree to be checked
     */
    private fun getSelectedBspTargetFrom(tree: Tree): BuildTarget? {
      val selected: DefaultMutableTreeNode? = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
      val userObject: Any? = selected?.userObject
      if (userObject is TargetNodeData) {
        return userObject.target
      }
      return null
    }
  }
}
