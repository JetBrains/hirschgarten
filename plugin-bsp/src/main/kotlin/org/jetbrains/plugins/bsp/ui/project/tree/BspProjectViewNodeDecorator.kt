package org.jetbrains.plugins.bsp.ui.project.tree

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.workspaceModel.ide.getInstance
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.flow.open.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import javax.swing.Icon

public class BspProjectViewNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  private var loadedModulesBaseDirectories: Set<VirtualFile> = emptySet()

  private lateinit var buildToolIcon: Icon

  init {
    if (project.isBspProject) {
      loadedModulesBaseDirectories = calculateAllTargetsBaseDirectories()

      val magicMetaModel = MagicMetaModelService.getInstance(project).value
      magicMetaModel.registerTargetLoadListener {
        loadedModulesBaseDirectories = calculateAllTargetsBaseDirectories()
      }

      val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
      buildToolIcon = assetsExtension.icon
    }
  }

  private fun calculateAllTargetsBaseDirectories(): Set<VirtualFile> {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    val virtualFileUrlManager = VirtualFileUrlManager.getInstance(project)

    return magicMetaModel.getAllLoadedTargets()
      .mapNotNull { it.baseDirectory }
      .map { virtualFileUrlManager.fromUrl(it) }
      .mapNotNull { it.virtualFile }
      .toSet()
  }

  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (project.isBspProject) {
      decorateForBsp(node, data)
    }
  }

  private fun decorateForBsp(node: ProjectViewNode<*>, data: PresentationData) {
    if (node is PsiDirectoryNode) {
      decorateIfBspTarget(node, data)
      data.removeJavaHackSourceRoot()
    }
  }

  private fun decorateIfBspTarget(node: PsiDirectoryNode, data: PresentationData) {
    if (node.isBspModule()) {
      data.addIconAndHighlight()
    }
  }

  private fun PsiDirectoryNode.isBspModule(): Boolean =
    loadedModulesBaseDirectories.contains(virtualFile)

  private fun PresentationData.addIconAndHighlight() {
    setIcon(buildToolIcon)
    addText(presentableText, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
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
