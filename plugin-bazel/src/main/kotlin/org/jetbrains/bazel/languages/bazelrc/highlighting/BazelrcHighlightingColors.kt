package org.jetbrains.bazel.languages.bazelrc.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object BazelrcHighlightingColors {
  val COMMAND = createTextAttributesKey("BAZELRC_COMMAND", DefaultLanguageHighlighterColors.KEYWORD)
  val IMPORT = createTextAttributesKey("BAZELRC_IMPORT", DefaultLanguageHighlighterColors.STATIC_METHOD)
  val STRING = createTextAttributesKey("BAZELRC_STRING", DefaultLanguageHighlighterColors.STRING)
  val NUMBER = createTextAttributesKey("BAZELRC_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
  val CONST = createTextAttributesKey("BAZELRC_CONST", DefaultLanguageHighlighterColors.CONSTANT)
  val COMMENT = createTextAttributesKey("BAZELRC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val IDENTIFIER = createTextAttributesKey("BAZELRC_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
  val UNKNOWN_FLAG = createTextAttributesKey("BAZELRC_UNKNOWN_FLAG", CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES)
}
