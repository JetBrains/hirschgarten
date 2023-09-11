package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension

import ch.epfl.scala.bsp4j.PackageFeatures
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.components.JBLabel
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.PlatformIcons
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import java.awt.Component
import java.awt.event.MouseListener
import javax.swing.JCheckBox
import javax.swing.JTree
import javax.swing.SwingConstants
import javax.swing.tree.*
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.tasks.UpdateFeaturesTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BspPanelComponent

public class FeaturesTree(
    private val project: Project,
    private val packagesFeatures: Collection<PackageFeatures>,
) : FeaturesContainer {
    private val rootNode = DefaultMutableTreeNode(PackageNodeData("[root]"))
    private val cellRenderer = FeaturesTreeCellRenderer()
    public val treeComponent: Tree = Tree(rootNode)

    init {
        treeComponent.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        treeComponent.cellRenderer = cellRenderer
        treeComponent.isRootVisible = false
        generateTree()
        treeComponent.expandPath(TreePath(rootNode.path))
    }

    private fun generateTree() {
        val sortedPackages = packagesFeatures.sortedBy {
                    it.packageId
            }
        rootNode.removeAllChildren()
        sortedPackages
            .mapNotNull { generatePackageNode(it) }
            .forEach { rootNode.add(it) }
    }

    private fun generatePackageNode(
        `package`: PackageFeatures,
    ): DefaultMutableTreeNode? {
        // Skip generating node for package with no features
        if (`package`.availableFeatures.isEmpty()) {
            return null
        }

        val node = DefaultMutableTreeNode()
        node.userObject = PackageNodeData(`package`.packageId)

        val enabledFeaturesForPackage = FeaturesService.getInstance(project).value.getEnabledFeaturesForPackage(`package`.packageId)
        val childrenFeatureNodesList =
            generateFeatureNodes(`package`.availableFeatures, enabledFeaturesForPackage).toMutableList()
        childrenFeatureNodesList.forEachIndexed { index, child ->
            if (child is MutableTreeNode) {
                node.insert(child, index)
            }
        }
        return node
    }

    private fun generateFeatureNodes(
        availableFeatures: Map<String, List<String>>,
        enabledFeatures: Set<String>,
    ): List<TreeNode> {

        val featureNodes = mutableListOf<TreeNode>()
        for (feature in availableFeatures) {
            val isEnabled = feature.key in enabledFeatures
            val featureNode = DefaultMutableTreeNode()
            featureNode.userObject = FeatureNodeData(feature.key, feature.value, isEnabled)
            featureNodes.add(featureNode)
        }
        return featureNodes
    }

    override fun isEmpty(): Boolean = rootNode.isLeaf

    override fun addMouseListener(mouseListener: (FeaturesContainer) -> MouseListener) {
        treeComponent.addMouseListener(mouseListener(this))
    }

    private fun getSelectedFeatureAndPackage(): Selection? {
        val selectedFeature = treeComponent.lastSelectedPathComponent as? DefaultMutableTreeNode
        val featureNode = selectedFeature?.userObject as? FeatureNodeData

        val packageNodeTreeReference = selectedFeature?.parent as? DefaultMutableTreeNode
        val packageNode = packageNodeTreeReference?.userObject as? PackageNodeData
        return if (featureNode != null && packageNode != null) {
            Selection(featureNode, packageNode, packageNodeTreeReference)
        } else {
            null
        }
    }

    private fun updateUiByTogglingGivenFeatures(featuresToToggle: Set<String>, packageTreeNode: DefaultMutableTreeNode, enabling: Boolean) {
        val packageAllFeaturesNodes = packageTreeNode.children().toList()
            .map { it as DefaultMutableTreeNode }
            .map { it.userObject as FeatureNodeData }

        featuresToToggle.forEach { f ->
            val featureNode = packageAllFeaturesNodes.find { it.keyFeature == f }!!
            if (enabling) featureNode.enable() else featureNode.disable()
        }
    }
    private fun handleTogglingFeature(selection: Selection, project: Project) {
        val featuresServiceValue = FeaturesService.getInstance(project).value

        val enablingFeature = !selection.feature.isEnabled
        val featuresToToggle = mutableSetOf(selection.feature.keyFeature)
        val featureMap = if (enablingFeature)
            featuresServiceValue.getFlattenedEnablingMap(selection.`package`.id)
        else
            featuresServiceValue.getFlattenedDisablingMap(selection.`package`.id)
        featuresToToggle += featureMap[selection.feature.keyFeature]!!

        val state = featuresServiceValue.enabled.getOrPut(selection.`package`.id) { mutableSetOf() }
        if (enablingFeature)
            state += featuresToToggle
        else
            state -= featuresToToggle

        updateUiByTogglingGivenFeatures(featuresToToggle, selection.packageNodeTreeReference, enablingFeature)
        updateServerState(project, selection.`package`.id, state)
    }
    private fun updateServerState(project: Project, packageId: String, enabledFeaturesSet: Set<String>) {
        BspCoroutineService.getInstance(project).start {
            UpdateFeaturesTask(project, packageId, enabledFeaturesSet).execute()?.let { statusCode ->
                if (statusCode != StatusCode.OK) {
                    log.warn("Updating features state failed")
                }
            }
        }
    }
    override fun toggleFeatureIfSelected() {
        val selection = getSelectedFeatureAndPackage()
        if (selection != null) {
            handleTogglingFeature(selection, project)
            treeComponent.repaint()
        }
    }

    private companion object {
        private val log = logger<BspPanelComponent>()
    }
}
private data class PackageNodeData(
    val id: String,
) {
    override fun toString(): String = id
}
private data class FeatureNodeData(
    val keyFeature: String,
    val dependants: List<String>,
    var isEnabled: Boolean = false,
    ) {
    override fun toString(): String {
        var returnVal = keyFeature

        if (dependants.isNotEmpty()) {
            returnVal += ": $dependants"
        }
        return returnVal
    }

    fun enable() {
        isEnabled = true
    }

    fun disable() {
        isEnabled = false
    }
}

private class FeaturesTreeCellRenderer: TreeCellRenderer {
    private val checkBoxForFeatureNodeData = JCheckBox()
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
            is PackageNodeData -> JBLabel(
                userObject.id,
                BspPluginIcons.cargoPackage,
                SwingConstants.LEFT,
            )

            is FeatureNodeData -> {
                checkBoxForFeatureNodeData.text = userObject.toString()
                checkBoxForFeatureNodeData.isSelected = userObject.isEnabled
                return checkBoxForFeatureNodeData
            }

            else -> JBLabel(
                "[not renderable]",
                PlatformIcons.ERROR_INTRODUCTION_ICON,
                SwingConstants.LEFT,
            )
        }
    }
}

private class Selection(val feature: FeatureNodeData, val `package`: PackageNodeData, val packageNodeTreeReference: DefaultMutableTreeNode)
