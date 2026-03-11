package org.jetbrains.bazel.languages.projectview.references

import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectViewCodeInsightSupport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem

@ApiStatus.Internal
class ProjectViewLabelReference(val element: ProjectViewPsiSectionItem) :
  PsiReferenceBase<ProjectViewPsiSectionItem>(element, TextRange(0, element.textLength)) {
  override fun resolve(): PsiElement? {
    val project = element.project
    if (!project.isBazelProject) return null
    val text = element.text ?: return null
    val label = Label.parseOrNull(text) ?: return null
    return project.service<ProjectViewCodeInsightSupport>()
      .resolvePsiFromLabel(label, null, false)
  }
}
