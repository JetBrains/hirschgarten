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
import org.jetbrains.bazel.languages.projectview.ProjectViewSections
import org.jetbrains.bazel.languages.projectview.ScalarSection
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile

internal class ProjectViewSectionCompletionContributor : CompletionContributor() {
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
      .andNot(psiElement().afterLeaf(psiElement(ProjectViewTokenType.COLON)))
      .andNot(psiComment())

  private class ProjectViewSectionCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
      parameters: CompletionParameters,
      context: ProcessingContext,
      result: CompletionResultSet,
    ) {
      result.addElement(LookupElementBuilder.create("import").withIcon(PlatformIcons.FUNCTION_ICON))
      result.addAllElements(
        ProjectViewSections.REGISTERED_SECTIONS.map {
          sectionLookupElement(it.name, it is ScalarSection<*>)
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
