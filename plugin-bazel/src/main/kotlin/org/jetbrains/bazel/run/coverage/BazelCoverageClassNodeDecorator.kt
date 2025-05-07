package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.PlatformUtils
import org.jetbrains.bazel.sdkcompat.shouldShowCoverageInProjectView

class BazelCoverageClassNodeDecorator(private val project: Project) : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    if (!project.shouldShowCoverageInProjectView() || PlatformUtils.isCLion()) return
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

interface BazelCoverageClassNodeDecoratorPsiElementClassifier {
  fun shouldShowCoverageForElement(psiElement: PsiElement): Boolean

  companion object {
    val ep =
      ExtensionPointName.create<BazelCoverageClassNodeDecoratorPsiElementClassifier>(
        "org.jetbrains.bazel.bazelCoverageClassNodeDecoratorPsiElementClassifier",
      )
  }
}
