package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSections
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionName

internal class ProjectViewPrefixMatcher(prefix: String) : PrefixMatcher(prefix) {
  override fun prefixMatches(name: String): Boolean = name.contains(prefix, ignoreCase = true)

  override fun cloneWithPrefix(newPrefix: String): PrefixMatcher = ProjectViewPrefixMatcher(newPrefix)
}

internal class ProjectViewSectionItemCompletionContributor : CompletionContributor() {
  class AutoPopup : TypedHandlerDelegate() {
    private val acceptedChars = listOf('/', ':', '_')

    override fun checkAutoPopup(
      charTyped: Char,
      project: Project,
      editor: Editor,
      file: PsiFile,
    ): Result {
      if (file !is ProjectViewPsiFile || (!charTyped.isLetterOrDigit() && charTyped !in acceptedChars)) return Result.CONTINUE

      AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
      return Result.CONTINUE
    }
  }

  init {
    for (section in ProjectViewSections.REGISTERED_SECTIONS) {
      if (section.completionProvider != null) {
        extend(
          CompletionType.BASIC,
          sectionItemElement(section.name),
          section.completionProvider,
        )
      }
    }

    extend(
      CompletionType.BASIC,
      importElement(),
      ImportCompletionProvider(),
    )
  }

  private fun sectionItemElement(sectionName: String) =
    psiElement()
      .withLanguage(ProjectViewLanguage)
      .withSuperParent(
        2,
        psiElement(ProjectViewPsiSection::class.java).withFirstChild(
          psiElement(ProjectViewPsiSectionName::class.java).withText(sectionName),
        ),
      )

  private fun importElement() =
    psiElement()
      .withLanguage(ProjectViewLanguage)
      .inside(ProjectViewPsiImport::class.java)
}
