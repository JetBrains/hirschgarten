package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.model.Pointer
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.createSmartPointer
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

internal class BazelRuleCallDocumentationTarget(val targetExpression: StarlarkCallExpression) : DocumentationTarget {

  override fun createPointer(): Pointer<out DocumentationTarget> {
    val elementPtr = targetExpression.createSmartPointer()
    return Pointer {
      val element = elementPtr.dereference() ?: return@Pointer null
      BazelRuleCallDocumentationTarget(element)
    }
  }

  override fun computePresentation(): TargetPresentation {
    val presentation = targetExpression.presentation
    return TargetPresentation
      .builder(presentation?.presentableText.orEmpty())
      .icon(presentation?.getIcon(false) ?: BazelPluginIcons.bazel)
      .locationText(presentation?.locationString, BazelPluginIcons.bazel)
      .presentation()
  }

  override fun computeDocumentationHint(): String = HtmlChunk
    .tag("pre")
    .style("white-space: pre; overflow-x: auto")
    .child(targetExpression.asHighlightedHtmlWithLabelLinks())
    .toString()

  override fun computeDocumentation(): DocumentationResult {
    val pre = HtmlChunk.tag("pre")
      .style("white-space: pre; overflow-x: auto")
      .child(targetExpression.asHighlightedHtmlWithLabelLinks())
    val definition = HtmlChunk.div().setClass(DocumentationMarkup.CLASS_DEFINITION).child(pre)
    return DocumentationResult.documentation(definition.toString())
  }

  private fun StarlarkElement.asHighlightedHtmlWithLabelLinks(): HtmlChunk = asHighlightedHtml {
    val literal = it.parent as? StarlarkStringLiteralExpression ?: return@asHighlightedHtml null
    StarlarkDocumentationLinks.forTargetLabel(literal)
  }
}
