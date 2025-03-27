package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.bazel.sdkcompat.shouldShowCoverageInProjectView

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

    val psiClassClass = runCatching { Class.forName("com.intellij.psi.PsiClass") }.getOrNull()
    val ktClassOrObjectClass = runCatching { Class.forName("org.jetbrains.kotlin.psi.KtClassOrObject") }.getOrNull()

    if (psiClassClass == null || ktClassOrObjectClass == null) return
    if (!psiClassClass.isInstance(psiElement) && !ktClassOrObjectClass.isInstance(psiElement)) return

    val psiFile = psiElement.containingFile ?: return
    val coverageDataManager = CoverageDataManager.getInstance(project)
    val suite = coverageDataManager.currentSuitesBundle ?: return
    val annotator = suite.getAnnotator(project) as? BazelCoverageAnnotator ?: return
    annotator.getFileCoverageInformationString(psiFile, suite, coverageDataManager)?.let {
      data.locationString = it
    }
  }
}
