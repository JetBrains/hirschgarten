package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageOptionsProvider
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

private class BazelCoverageClassNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (!CoverageOptionsProvider.getInstance(project).showInProjectView()) return

    val nodeValue = node.value
    val psiElement =
      when (nodeValue) {
        is SmartPsiElementPointer<*> -> nodeValue.element
        is PsiElement -> nodeValue
        else -> null
      } ?: return

    if (BazelCoverageClassNodeDecoratorPsiElementClassifier.ep.extensionList.none { it.shouldShowCoverageForElement(psiElement) }) return

    val psiFile = psiElement.containingFile ?: return
    val coverageDataManager = CoverageDataManager.getInstance(project)
    val suite = coverageDataManager.currentSuitesBundle ?: return
    val annotator = suite.getAnnotator(project) as? BazelCoverageAnnotator ?: return
    annotator.getFileCoverageInformationString(psiFile, suite, coverageDataManager)?.let {
      data.locationString = it
    }
  }
}

internal interface BazelCoverageClassNodeDecoratorPsiElementClassifier {
  fun shouldShowCoverageForElement(psiElement: PsiElement): Boolean

  companion object {
    val ep =
      ExtensionPointName<BazelCoverageClassNodeDecoratorPsiElementClassifier>(
        "org.jetbrains.bazel.bazelCoverageClassNodeDecoratorPsiElementClassifier",
      )
  }
}
