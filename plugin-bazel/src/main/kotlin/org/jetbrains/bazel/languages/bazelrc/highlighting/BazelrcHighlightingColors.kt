package org.jetbrains.bazel.languages.bazelrc.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object BazelrcHighlightingColors {
  val KEYWORD = createTextAttributesKey("BAZELRC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
  val STRING = createTextAttributesKey("BAZELRC_STRING", DefaultLanguageHighlighterColors.STRING)
  val NUMBER = createTextAttributesKey("BAZELRC_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
  val CONST = createTextAttributesKey("BAZELRC_CONST", DefaultLanguageHighlighterColors.CONSTANT)
  val COMMENT = createTextAttributesKey("BAZELRC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val IDENTIFIER = createTextAttributesKey("BAZELRC_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
}
