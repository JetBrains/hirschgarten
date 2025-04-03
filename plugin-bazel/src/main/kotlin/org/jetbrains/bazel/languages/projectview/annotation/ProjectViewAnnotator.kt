package org.jetbrains.bazel.languages.projectview.annotation

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.language.ProjectViewSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.kotlin.idea.base.codeInsight.handlers.fixers.range

class ProjectViewAnnotator : Annotator {
  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    val annotation: Annotation? =
      if (element is ProjectViewPsiSection) {
        // Parse the item.
        val name = element.firstChild.text
        val item = element.lastChild
        ProjectViewSection.KEYWORD_MAP[name]?.let { parser ->
          val res = parser.parseItem(item.text)
          when (res) {
            is ProjectViewSection.Parser.ParsingResult.OK -> null
            is ProjectViewSection.Parser.ParsingResult.NoItemError -> {
              Annotation(
                "No item!",
                element.range,
                HighlightSeverity.ERROR,
              )
            }
            is ProjectViewSection.Parser.ParsingResult.Error ->
              Annotation(
                res.message,
                item.range,
                HighlightSeverity.ERROR,
              )
          }
        }
      } else {
        null
      }

    annotation?.let {
      holder
        .newAnnotation(annotation.severity, annotation.message)
        .range(annotation.range)
        .create()
    }
  }

  data class Annotation(
    val message: String,
    val range: TextRange,
    val severity: HighlightSeverity,
  )
}
