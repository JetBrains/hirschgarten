package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiImport
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range

class ProjectViewAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is ProjectViewPsiSection) {
      // Check if the section is empty.
      ProjectViewSection.KEYWORD_MAP[element.firstChild.text]?.let { section ->
        val isSectionEmpty: Boolean = if (section.isList) {
          element.children.isEmpty()
        } else {
          element.lastChild.firstChild == null
        }

        if (isSectionEmpty) {
          createErrorAnnotation(holder, "No items!", element.range)
        }
      }
    } else if (element is ProjectViewPsiSectionItem) {
      // Parse the item.
      ProjectViewSection.KEYWORD_MAP[element.parent.firstChild.text]?.let { section ->
        val res = section.parseItem(element.text)
        when (res) {
          is ProjectViewSection.ParsingResult.OK -> {
            createHighlightAnnotation(holder, section.getHighlightColor(), element.range)
          }
          is ProjectViewSection.ParsingResult.Error ->
            createErrorAnnotation(holder, res.message, element.range)
        }
      }
    } else if (element is ProjectViewPsiImport && element.lastChild.firstChild == null) {
      createErrorAnnotation(holder, "No import path!", element.range)
    }
  }

  private fun createErrorAnnotation(holder: AnnotationHolder, message: String, range: TextRange) {
    holder.newAnnotation(HighlightSeverity.ERROR, message).range(range).create()
  }

  private fun createHighlightAnnotation(holder: AnnotationHolder, textAttributesKey: TextAttributesKey, range: TextRange) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(textAttributesKey).range(range).create()
  }

  data class Annotation(
    val message: String,
    val range: TextRange,
    val severity: HighlightSeverity,
  )
}
