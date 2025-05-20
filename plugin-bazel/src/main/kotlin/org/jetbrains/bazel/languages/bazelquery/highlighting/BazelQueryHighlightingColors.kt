package org.jetbrains.bazel.languages.bazelquery.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

object BazelQueryHighlightingColors {
  val COMMAND = createTextAttributesKey("BAZELQUERY_COMMAND", DefaultLanguageHighlighterColors.IDENTIFIER)
  val WORD = createTextAttributesKey("BAZELQUERY_WORD", DefaultLanguageHighlighterColors.STRING)
  val PROMPT = createTextAttributesKey("BAZELQUERY_PROMPT", DefaultLanguageHighlighterColors.KEYWORD)
  val COMMENT = createTextAttributesKey("BAZELQUERY_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val OPERATION = createTextAttributesKey("BAZELQUERY_OPERATION", DefaultLanguageHighlighterColors.IDENTIFIER)
  val FLAG = createTextAttributesKey("BAZELQUERY_FLAG", DefaultLanguageHighlighterColors.LINE_COMMENT)
  val FLAG_VAL = createTextAttributesKey("BAZELQUERY_FLAG_VAL", DefaultLanguageHighlighterColors.CONSTANT)

  val INTEGER = createTextAttributesKey("BAZELQUERY_INTEGER", DefaultLanguageHighlighterColors.NUMBER)
}
