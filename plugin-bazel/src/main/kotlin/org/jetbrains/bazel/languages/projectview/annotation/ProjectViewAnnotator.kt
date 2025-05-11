package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.language.ProjectViewImport
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSectionParser
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range

class ProjectViewAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    when (element) {
      is ProjectViewPsiSection -> {
        ProjectViewSection.KEYWORD_MAP[element.getKeyword()]?.let { parser ->
          val itemElements = element.getItems()
          parser.parse(itemElements.map { it.text }).fold(
            onSuccess = {
              it.textAttributesKey?.let { textAttributesKey ->
                itemElements.forEach { itemElement ->
                  createInformationAnnotation(
                    holder,
                    textAttributesKey,
                    itemElement.range,
                  )
                }
              }
            },
            onFailure = {
              val errorScope =
                when (it) {
                  is ProjectViewSectionParser.ScalarValueException -> itemElements.first()
                  else -> element
                }
              createErrorAnnotation(holder, it.message ?: "", errorScope.range)
            },
          )
        }
      }
      is ProjectViewPsiImport -> {
        ProjectViewImport.KEYWORD_MAP[element.getKeyword()]?.let { import ->
          import.parse(element.getPath()).onFailure { createErrorAnnotation(holder, it.message ?: "", element.range) }
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
