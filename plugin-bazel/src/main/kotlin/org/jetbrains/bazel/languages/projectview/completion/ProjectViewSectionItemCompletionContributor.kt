package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName

class ProjectViewSectionItemCompletionContributor : CompletionContributor() {
  init {
    for (metadata in ProjectViewSection.KEYWORD_MAP.values) {
      if (metadata.completionProvider != null) {
        extend(
          CompletionType.BASIC,
          sectionItemElement(metadata.sectionName),
          metadata.completionProvider,
        )
      }
    }
  }

  private fun sectionItemElement(sectionName: String) = psiElement()
    .withLanguage(ProjectViewLanguage)
    .withSuperParent(
      2,
      psiElement(ProjectViewPsiSection::class.java).withFirstChild(
        psiElement(ProjectViewPsiSectionName::class.java).withText(sectionName)
      )
    )
}
