package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object StarlarkHighlightingColors {
    val KEYWORD =
        TextAttributesKey.createTextAttributesKey("STARLARK_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    val STRING = TextAttributesKey.createTextAttributesKey("STARLARK_STRING", DefaultLanguageHighlighterColors.STRING)
    val NUMBER = TextAttributesKey.createTextAttributesKey("STARLARK_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
    val LINE_COMMENT =
        TextAttributesKey.createTextAttributesKey("STARLARK_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val SEMICOLON =
        TextAttributesKey.createTextAttributesKey("STARLARK_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val COMMA = TextAttributesKey.createTextAttributesKey("STARLARK_COMMA", DefaultLanguageHighlighterColors.COMMA)
    val DOT = TextAttributesKey.createTextAttributesKey("STARLARK_DOT", DefaultLanguageHighlighterColors.DOT)
    val PARENTHESES =
        TextAttributesKey.createTextAttributesKey("STARLARK_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)
    val BRACKETS =
        TextAttributesKey.createTextAttributesKey("STARLARK_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val IDENTIFIER =
        TextAttributesKey.createTextAttributesKey("STARLARK_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
}