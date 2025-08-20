package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.language.isSectionSupported
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

class ProjectViewAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is ProjectViewPsiSection) {
      return
    }

    val sectionName = element.getKeyword().text
     if (!isSectionSupported(sectionName)) {
      holder.annotateWarning(element.firstChild, ProjectViewBundle.getMessage("annotator.unsupported.section.warning"))
     }
  }

  private fun AnnotationHolder.annotateWarning(element: PsiElement, message: String) {
    newAnnotation(HighlightSeverity.WARNING, message).range(element).create()
  }
}
