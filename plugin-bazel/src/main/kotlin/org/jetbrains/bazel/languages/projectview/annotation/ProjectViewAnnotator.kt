package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range

class ProjectViewAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is ProjectViewPsiSection) {
      // Check if the section is empty.
      ProjectViewSection.KEYWORD_MAP[element.firstChild.text]?.let { parser ->
        val isSectionEmpty: Boolean = when (parser) {
          is ProjectViewSection.Parser.Scalar -> element.lastChild.firstChild == null
          is ProjectViewSection.Parser.List -> element.children.isEmpty()
        }

        if (isSectionEmpty) {
          createErrorAnnotation(holder, "No items!", element.range)
        }
      }
    } else if (element is ProjectViewPsiSectionItem) {
      // Parse the item.
      ProjectViewSection.KEYWORD_MAP[element.parent.firstChild.text]?.let { parser ->
        element.firstChild?.let { item ->
          val res = parser.parseItem(item.text)
          when (res) {
            is ProjectViewSection.Parser.ParsingResult.OK -> null
            is ProjectViewSection.Parser.ParsingResult.Error ->
              createErrorAnnotation(holder, res.message, item.range)
          }
        }
      }
    }
  }

  private fun createErrorAnnotation(holder: AnnotationHolder, message: String, range: TextRange) {
    holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create()
  }

  data class Annotation(
    val message: String,
    val range: TextRange,
    val severity: HighlightSeverity,
  )
}
