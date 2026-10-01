package org.jetbrains.bazel.java.ui.projectTree

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.TreeViewUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.ui.projectTree.BazelProjectViewDirectoryHelper

internal class BazelJavaPackageNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (project.isBazelProject && node is PsiDirectoryNode && node.settings.isFlattenPackages) {
      val directory = node.value ?: return
      val parentDirectory = directory.parentDirectory ?: return
      val helper = BazelProjectViewDirectoryHelper(project)
      if (helper.getPackageName(parentDirectory) == null) {
        return
      }

      val packageName = helper.getPackageName(directory) ?: return
      if (packageName.isEmpty()) {
        return
      }

      data.clearText()
      val psiPackage =
        if (node.settings.isAbbreviatePackageNames) {
          JavaPsiFacade.getInstance(project).findPackage(packageName)
        } else null
      data.presentableText = psiPackage?.let { TreeViewUtil.calcAbbreviatedPackageFQName(it) } ?: packageName
    }
  }
}
