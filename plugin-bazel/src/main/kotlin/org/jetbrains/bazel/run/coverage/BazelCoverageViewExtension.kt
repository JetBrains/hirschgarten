package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageAnnotator
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.view.CoverageListNode
import com.intellij.coverage.view.CoverageListRootNode
import com.intellij.coverage.view.DirectoryCoverageViewExtension
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.rootDir

class BazelCoverageViewExtension(
  private val project: Project,
  coverageAnnotator: CoverageAnnotator,
  private val bundle: CoverageSuitesBundle,
) : DirectoryCoverageViewExtension(project, coverageAnnotator, bundle) {
  private val roots: List<VirtualFile> = BazelCoverageSuite.getRoots(bundle)

  override fun createRootNode(): AbstractTreeNode<*> =
    if (roots.size == 1) {
      object : CoverageListRootNode(project, roots.single().toPsiDirectory(), bundle) {
        override fun getChildren(): List<AbstractTreeNode<*>> = getChildrenNodes(this)
      }
    } else {
      CoverageListRootNode(project, project.rootDir.toPsiDirectory(), bundle)
    }

  override fun getChildrenNodes(node: AbstractTreeNode<*>?): List<AbstractTreeNode<*>> {
    if (node is CoverageListRootNode && roots.size != 1) {
      return roots.map { root ->
        CoverageListNode(project, root.toPsiDirectory(), bundle)
      }
    }
    return super.getChildrenNodes(node)
  }

  private fun VirtualFile.toPsiDirectory(): PsiDirectory {
    val directory = if (!this.isDirectory) this.parent else this
    return checkNotNull(PsiManager.getInstance(myProject).findDirectory(directory))
  }
}
