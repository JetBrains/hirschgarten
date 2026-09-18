package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.lang.documentation.QuickDocHighlightingHelper
import com.intellij.model.Pointer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors

@Suppress("UnstableApiUsage")
internal class BazelGlobalFunctionDocumentationTarget(symbol: BazelGlobalFunctionDocumentationSymbol) :
  DocumentationTarget,
  Pointer<BazelGlobalFunctionDocumentationTarget> {
  val symbolPtr = symbol.createPointer()

  override fun createPointer() = this

  override fun dereference(): BazelGlobalFunctionDocumentationTarget = symbolPtr.dereference().documentationTarget

  override fun computePresentation(): TargetPresentation =
    symbolPtr.dereference().run {
      TargetPresentation.builder(function.name).presentation()
    }

  private fun computeFunctionDefinition(function: BazelGlobalFunction): String {
    val functionName = QuickDocHighlightingHelper.getStyledFragment(function.name, StarlarkHighlightingColors.FUNCTION_DECLARATION)
    val comma = QuickDocHighlightingHelper.getStyledFragment(", ", StarlarkHighlightingColors.COMMA)
    val openParen = QuickDocHighlightingHelper.getStyledFragment("(", StarlarkHighlightingColors.PARENTHESES)
    val closeParen = QuickDocHighlightingHelper.getStyledFragment(")", StarlarkHighlightingColors.PARENTHESES)
    val params =
      function.params.joinToString(comma) { param ->
        val name = QuickDocHighlightingHelper.getStyledFragment(param.name, StarlarkHighlightingColors.NAMED_ARGUMENT)
        if (param.defaultValue != null) {
          val value = QuickDocHighlightingHelper.getStyledFragment(param.defaultValue, DefaultLanguageHighlighterColors.STRING)
          "$name = $value"
        } else {
          name
        }
      }
    return "$functionName$openParen$params$closeParen"
  }

  @Suppress("HardCodedStringLiteral")
  override fun computeDocumentation(): DocumentationResult? =
    symbolPtr.dereference().run {
      val functionDefinition = computeFunctionDefinition(function)
      val html = function.doc ?: ""
      DocumentationResult.documentation("<pre>$functionDefinition</pre><hr/>$html")
    }
}
