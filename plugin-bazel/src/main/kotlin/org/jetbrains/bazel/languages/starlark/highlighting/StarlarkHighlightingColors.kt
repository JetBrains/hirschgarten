package org.jetbrains.bazel.languages.starlark.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object StarlarkHighlightingColors {
  val KEYWORD = createTextAttributesKey("STARLARK_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
  val STRING = createTextAttributesKey("STARLARK_STRING", DefaultLanguageHighlighterColors.STRING)
  val NUMBER = createTextAttributesKey("STARLARK_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
  val LINE_COMMENT = createTextAttributesKey("STARLARK_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val SEMICOLON = createTextAttributesKey("STARLARK_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
  val COMMA = createTextAttributesKey("STARLARK_COMMA", DefaultLanguageHighlighterColors.COMMA)
  val DOT = createTextAttributesKey("STARLARK_DOT", DefaultLanguageHighlighterColors.DOT)
  val PARENTHESES = createTextAttributesKey("STARLARK_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
  val BRACKETS = createTextAttributesKey("STARLARK_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
  val IDENTIFIER = createTextAttributesKey("STARLARK_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
  val FUNCTION_DECLARATION =
    createTextAttributesKey("STARLARK_FUNCTION_DECLARATION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
  val NAMED_ARGUMENT = createTextAttributesKey("STARLARK_NAMED_ARGUMENT")
}
