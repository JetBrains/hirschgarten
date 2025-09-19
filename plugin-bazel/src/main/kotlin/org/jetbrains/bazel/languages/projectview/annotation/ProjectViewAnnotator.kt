package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.ProjectViewBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewSections
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

class ProjectViewAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element !is ProjectViewPsiSection) {
      return
    }

    val sectionName = element.getKeyword().text
    val section = ProjectViewSections.getSectionByName(sectionName)
    if (section == null) {
      holder.annotateWarning(element.firstChild, ProjectViewBundle.getMessage("annotator.unsupported.section.warning"))
      return
    }
    for (value in element.getItems()) {
      section.annotateValue(value, holder)
    }
  }

  private fun AnnotationHolder.annotateWarning(element: PsiElement, message: String) {
    newAnnotation(HighlightSeverity.WARNING, message).range(element).create()
  }
}
