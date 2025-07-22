package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.highlighting.StarlarkHighlightingColors
import java.awt.Color

@Suppress("UnstableApiUsage")
class BazelGlobalFunctionDocumentationTarget(symbol: BazelGlobalFunctionDocumentationSymbol) :
  DocumentationTarget,
  Pointer<BazelGlobalFunctionDocumentationTarget> {
  val symbolPtr = symbol.createPointer()

  override fun createPointer() = this

  override fun dereference(): BazelGlobalFunctionDocumentationTarget? = symbolPtr.dereference().documentationTarget

  override fun computePresentation(): TargetPresentation =
    symbolPtr.dereference().run {
      TargetPresentation.builder(function.name).presentation()
    }

  val scheme = EditorColorsManager.getInstance().globalScheme

  private fun colorString(str: String, key: TextAttributesKey): String {
    val color = scheme.getAttributes(key, true)
    return "<span style='color:${color.foregroundColor.toRgbaHexString()}'>$str</span>"
  }

  private fun Color.toRgbaHexString() = "#%02X%02X%02X%02X".format(red, green, blue, alpha)

  private fun computeFunctionDefinition(function: BazelGlobalFunction): String {
    val functionName = colorString(function.name, StarlarkHighlightingColors.FUNCTION_DECLARATION)
    val comma = colorString(", ", StarlarkHighlightingColors.COMMA)
    val openParen = colorString("(", StarlarkHighlightingColors.PARENTHESES)
    val closeParen = colorString(")", StarlarkHighlightingColors.PARENTHESES)
    val params =
      function.params.joinToString(comma) { param ->
        val name = colorString(param.name, StarlarkHighlightingColors.NAMED_ARGUMENT)
        if (param.defaultValue != null) {
          val value = colorString(param.defaultValue, DefaultLanguageHighlighterColors.STRING)
          "$name = $value"
        } else {
          name
        }
      }
    return "$functionName$openParen$params$closeParen"
  }

  override fun computeDocumentation(): DocumentationResult? =
    symbolPtr.dereference().run {
      val functionDefinition = computeFunctionDefinition(function)
      val html = function.doc ?: ""
      DocumentationResult.documentation("<pre>$functionDefinition</pre><hr/>$html")
    }
}
