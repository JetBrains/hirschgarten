package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.bazel.sdkcompat.shouldShowCoverageInProjectView
import org.jetbrains.kotlin.psi.KtClassOrObject

class BazelCoverageClassNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (!project.shouldShowCoverageInProjectView()) return

    val nodeValue = node.value
    val psiElement =
      when (nodeValue) {
        is SmartPsiElementPointer<*> -> nodeValue.element
        is PsiElement -> nodeValue
        else -> null
      } ?: return

    if (psiElement !is PsiClass && psiElement !is KtClassOrObject) return

    val psiFile = psiElement.containingFile ?: return
    val coverageDataManager = CoverageDataManager.getInstance(project)
    val suite = coverageDataManager.currentSuitesBundle ?: return
    val annotator = suite.getAnnotator(project) as? BazelCoverageAnnotator ?: return
    annotator.getFileCoverageInformationString(psiFile, suite, coverageDataManager)?.let {
      data.locationString = it
    }
  }
}
