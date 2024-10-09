package org.jetbrains.plugins.bsp.ui.project.tree

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.isBspProject

class BspProjectViewNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (project.isBspProject) {
      decorateForBsp(node, data)
    }
  }

  private fun decorateForBsp(node: ProjectViewNode<*>, data: PresentationData) {
    if (node is PsiDirectoryNode) {
      data.clearPresentation()
    }
  }

  private fun PresentationData.clearPresentation() {
    clearText()
    locationString = ""
    setIcon(AllIcons.Nodes.Folder)
  }
}
