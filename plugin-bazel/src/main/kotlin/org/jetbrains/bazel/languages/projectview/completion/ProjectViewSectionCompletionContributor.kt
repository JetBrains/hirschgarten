package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiComment
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile

class ProjectViewSectionCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      projectViewSectionElement(),
      ProjectViewSectionCompletionProvider(),
    )
  }

  private fun projectViewSectionElement() =
    psiElement()
      .withLanguage(ProjectViewLanguage)
      .withSuperParent(2, ProjectViewPsiFile::class.java)
      .inFile(psiElement(ProjectViewPsiFile::class.java))
      .andNot(psiComment())

  private class ProjectViewSectionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
      parameters: CompletionParameters,
      context: ProcessingContext,
      result: CompletionResultSet,
    ) {
      result.addAllElements(
        ProjectViewSection.KEYWORD_MAP.map {
          sectionLookupElement(it.key, it.value is ProjectViewSection.Parser.Scalar)
        },
      )
    }

    private class SectionInsertionHandle<T : LookupElement>(val isScalar: Boolean) : InsertHandler<T> {
      override fun handleInsert(context: InsertionContext, item: T) {
        val editor = context.editor
        val document = editor.document
        document.insertString(context.tailOffset, ": ")
        if (isScalar) {
          editor.caretModel.moveToOffset(context.tailOffset)
        } else {
          document.insertString(context.tailOffset, "\n  ")
          editor.caretModel.moveToOffset(context.tailOffset)
        }
      }
    }

    private fun sectionLookupElement(sectionName: String, isScalar: Boolean): LookupElement =
      LookupElementBuilder
        .create(sectionName)
        .withIcon(PlatformIcons.FUNCTION_ICON)
        .withInsertHandler(SectionInsertionHandle(isScalar))
  }
}
