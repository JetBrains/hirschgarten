package org.jetbrains.bazel.languages.bazelquery.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.lexer.BazelqueryLexer

object BazelquerySyntaxHighlighter : SyntaxHighlighterBase() {
  private val keys =
    mapOf(
      *BazelqueryTokenSets.WORDS.types.map { it to BazelqueryHighlightingColors.WORD }.toTypedArray(),
      BazelqueryTokenTypes.COMMAND to BazelqueryHighlightingColors.COMMAND,
      BazelqueryTokenTypes.QUERY to BazelqueryHighlightingColors.PROMPT,
      BazelqueryTokenTypes.BAZEL to BazelqueryHighlightingColors.PROMPT,
      *BazelqueryTokenSets.OPERATIONS.types.map { it to BazelqueryHighlightingColors.OPERATION }.toTypedArray(),
      *BazelqueryTokenSets.FLAGS.types.map { it to BazelqueryHighlightingColors.FLAG }.toTypedArray(),
      *BazelqueryTokenSets.FLAG_VALS.types.map { it to BazelqueryHighlightingColors.FLAG_VAL }.toTypedArray(),
    )

  override fun getHighlightingLexer(): Lexer =
    BazelqueryLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
    pack(keys[tokenType])
}

private fun TokenSet.types(function: Any) {}

class BazelquerySyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
    BazelquerySyntaxHighlighter
}
