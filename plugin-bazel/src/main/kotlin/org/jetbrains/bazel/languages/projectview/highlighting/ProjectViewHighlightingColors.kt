package org.jetbrains.bazel.languages.projectview.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object ProjectViewHighlightingColors {
  val KEYWORD = createTextAttributesKey("PROJECTVIEW_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
  val STRING = createTextAttributesKey("PROJECTVIEW_STRING", DefaultLanguageHighlighterColors.STRING)
  val NUMBER = createTextAttributesKey("PROJECTVIEW_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
  val CONST = createTextAttributesKey("PROJECTVIEW_CONST", DefaultLanguageHighlighterColors.CONSTANT)
  val LINE_COMMENT = createTextAttributesKey("PROJECTVIEW_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val IDENTIFIER = createTextAttributesKey("PROJECTVIEW_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
}
