package org.jetbrains.plugins.bsp.ui.project.tree

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

public class BspProjectViewNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  private var loadedModulesBaseDirectories: Set<VirtualFile>? = null

  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (project.isBspProject) {
      initStateIfNeeded()
      decorateForBsp(node, data)
    }
  }

  private fun initStateIfNeeded() {
    if (loadedModulesBaseDirectories == null) {
      initState()
    }
  }

  private fun initState() {
    loadedModulesBaseDirectories = calculateAllTargetsBaseDirectories()

    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    magicMetaModel.registerTargetLoadListener {
      loadedModulesBaseDirectories = calculateAllTargetsBaseDirectories()
    }
  }

  private fun calculateAllTargetsBaseDirectories(): Set<VirtualFile> {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()

    return magicMetaModel.getAllLoadedTargets()
      .mapNotNull { it.baseDirectory }
      .map { virtualFileUrlManager.getOrCreateFromUri(it) }
      .mapNotNull { it.virtualFile }
      .toSet()
  }

  private fun decorateForBsp(node: ProjectViewNode<*>, data: PresentationData) {
    if (node is PsiDirectoryNode) {
      decorateIfBspTarget(node, data)
      data.removeJavaHackSourceRoot()
    }
  }

  private fun decorateIfBspTarget(node: PsiDirectoryNode, data: PresentationData) {
    when {
      node.isRootModule() -> data.addIconAndHighlight(node.virtualFile?.name)
      node.isBspModule() -> data.addIconAndHighlight(data.presentableText)
    }
  }

  private fun PsiDirectoryNode.isRootModule(): Boolean =
    project.rootDir == virtualFile

  private fun PsiDirectoryNode.isBspModule(): Boolean =
    loadedModulesBaseDirectories?.contains(virtualFile) == true

  private fun PresentationData.addIconAndHighlight(text: String?) {
    setIcon(project.assets.icon)
    addText(text, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
  }

  private fun PresentationData.removeJavaHackSourceRoot() {
    if (isJavaHackNode()) {
      clearPresentation()
    }
  }

  private fun PresentationData.isJavaHackNode() =
    locationString == "sources root"

  private fun PresentationData.clearPresentation() {
    clearText()
    locationString = ""
  }
}
