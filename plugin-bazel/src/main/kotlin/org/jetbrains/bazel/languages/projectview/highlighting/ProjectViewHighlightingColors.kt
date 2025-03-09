package org.jetbrains.bazel.languages.projectview.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object ProjectViewHighlightingColors {
  val COLON = createTextAttributesKey("PROJECTVIEW_COLON", DefaultLanguageHighlighterColors.SEMICOLON)
  val IDENTIFIER = createTextAttributesKey("PROJECTVIEW_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
  val KEYWORD = createTextAttributesKey("PROJECTVIEW_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
  val COMMENT = createTextAttributesKey("PROJECTVIEW_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
}
