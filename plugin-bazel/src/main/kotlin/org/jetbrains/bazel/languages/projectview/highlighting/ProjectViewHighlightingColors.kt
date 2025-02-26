package org.jetbrains.bazel.languages.projectview.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object ProjectViewHighlightingColors {
  val IDENTIFIER = createTextAttributesKey("PROJECTVIEW_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
  val COLON = createTextAttributesKey("PROJECTVIEW_COLON", DefaultLanguageHighlighterColors.SEMICOLON)
  val LINE_COMMENT = createTextAttributesKey("PROJECTVIEW_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
}
