package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.language.ProjectViewImport
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSectionParser.Item
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSectionParser.Result
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSectionParser.Scope
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range

class ProjectViewAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    when (element) {
      is ProjectViewPsiSection -> {
        val itemElements = element.getItems()
        val meta = ProjectViewSection.KEYWORD_MAP[element.getKeyword()]
        meta?.parser?.parse(itemElements.map { Item(it.text) })?.let {
          when (it) {
            is Result.Success -> {
              meta.textAttributesKey?.let { textAttributesKey ->
                itemElements.forEach { itemElement ->
                  createInformationAnnotation(
                    holder,
                    textAttributesKey,
                    itemElement.range,
                  )
                }
              }
            }
            is Result.Failure -> {
              val errorScope = it.scope
              val errorElement =
                when (errorScope) {
                  is Scope.Item -> itemElements[errorScope.id]
                  Scope.Section -> element
                }
              createErrorAnnotation(holder, it.message, errorElement.range)
            }
          }
        }
      }
      is ProjectViewPsiImport -> {
        ProjectViewImport.KEYWORD_MAP[element.getKeyword()]?.let { import ->
          import.parse(element.getPath()).onFailure {
            val message = it.message
            if (!message.isNullOrEmpty()) createErrorAnnotation(holder, message, element.range)
          }
        }
      }
    }
  }

  private fun createErrorAnnotation(
    holder: AnnotationHolder,
    message: String,
    range: TextRange,
  ) {
    holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create()
  }

  private fun createInformationAnnotation(
    holder: AnnotationHolder,
    textAttributesKey: TextAttributesKey,
    range: TextRange,
  ) {
    holder
      .newSilentAnnotation(HighlightSeverity.INFORMATION)
      .textAttributes(textAttributesKey)
      .range(range)
      .create()
  }
}
