package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.codeInsight.documentation.DocumentationManagerProtocol
import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.buildHtmlChunk
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendants
import com.intellij.psi.util.elementType
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkSyntaxHighlighter
import org.jetbrains.bazel.languages.starlark.highlighting.starlarkSemanticHighlightingColor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.isRuleTarget

internal object StarlarkDocumentationLinks {

  fun forTargetLabel(element: StarlarkStringLiteralExpression): String? {
    val referenced = element.getReference()?.resolve() ?: return null
    if (!referenced.isRuleTarget()) return null
    val labelText = element.getStringContents()
    return DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL + labelText
  }

  fun labelFrom(link: String): Label? {
    if (!link.startsWith(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)) return null
    val labelText = link.removePrefix(DocumentationManagerProtocol.PSI_ELEMENT_PROTOCOL)
    return Label.parseOrNull(labelText)
  }
}

internal fun StarlarkElement.asHighlightedHtml(linkBuilder: (PsiElement) -> String? = { null }): HtmlChunk = buildHtmlChunk {
  descendants()
    .filter { it.firstChild == null }
    .forEach { leaf ->
      val chunk = leaf.starlarkHighlightedHtmlChunk()
      val link = linkBuilder(leaf)
      append(if (link != null) HtmlChunk.link(link, chunk) else chunk)
    }
}

private fun PsiElement.starlarkHighlightedHtmlChunk(): HtmlChunk {
  val key = starlarkHighlightingColor() ?: return HtmlChunk.text(text)
  return HtmlChunk.raw(QuickDocHighlightingHelper.getStyledFragment(XmlStringUtil.escapeString(text), key))
}

private fun PsiElement.starlarkHighlightingColor(): TextAttributesKey? =
  starlarkSemanticHighlightingColor() ?: elementType?.let { StarlarkSyntaxHighlighter.getTokenHighlights(it).firstOrNull() }

