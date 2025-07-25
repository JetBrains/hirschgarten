package org.jetbrains.bazel.languages.projectview.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem
import org.jetbrains.bazel.languages.starlark.references.resolveLabel

class ProjectViewLabelReference(val element: ProjectViewPsiSectionItem) :
  PsiReferenceBase<ProjectViewPsiSectionItem>(element, TextRange(0, element.textLength)) {
  override fun resolve(): PsiElement? {
    val project = element.project
    if (!project.isBazelProject) return null
    val text = element.text ?: return null
    val label = Label.parseOrNull(text) ?: return null
    return resolveLabel(project, label, element.containingFile.originalFile.virtualFile, false)
  }
}
