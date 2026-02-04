package org.jetbrains.bazel.ui.projectTree

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.NewVirtualFile
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.commons.constants.Constants.SUPPORTED_CONFIG_FILE_NAMES
import org.jetbrains.bazel.config.isBazelProject

private class BazelProjectViewNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (project.isBazelProject) {
      decorateForBsp(node, data)
    }
  }

  private fun decorateForBsp(node: ProjectViewNode<*>, data: PresentationData) {
    if (node is PsiDirectoryNode) {
      data.removeJavaHackSourceRoot()
      data.setIconIfBazelDirectory(node)
    }
  }

  private fun PresentationData.removeJavaHackSourceRoot() {
    if (isJavaHackNode()) {
      clearPresentation()
    }
  }

  private fun PresentationData.isJavaHackNode() = locationString?.contains("sources root") == true

  private fun PresentationData.clearPresentation() {
    clearText()
    locationString = ""
    setIcon(AllIcons.Nodes.Folder)
  }

  private fun PresentationData.setIconIfBazelDirectory(node: PsiDirectoryNode) {
    node.virtualFile?.let { virtualFile ->
      if ((virtualFile is NewVirtualFile) && virtualFile.iterInDbChildren().any { it.name in SUPPORTED_CONFIG_FILE_NAMES }) {
        setIcon(BazelPluginIcons.bazelDirectory)
      }
    }
  }
}
