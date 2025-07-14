package org.jetbrains.bazel.languages.bazelquery.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes
import org.jetbrains.bazel.languages.bazelquery.lexer.BazelQueryLexer

object BazelQuerySyntaxHighlighter : SyntaxHighlighterBase() {
  private val keys =
    mapOf(
      *BazelQueryTokenSets.WORDS.types
        .map { it to BazelQueryHighlightingColors.WORD }
        .toTypedArray(),
      *BazelQueryTokenSets.PATTERNS.types
        .map { it to BazelQueryHighlightingColors.WORD }
        .toTypedArray(),
      BazelQueryTokenTypes.INTEGER to BazelQueryHighlightingColors.INTEGER,
      *BazelQueryTokenSets.OPERATIONS.types
        .map { it to BazelQueryHighlightingColors.OPERATION }
        .toTypedArray(),
      *BazelQueryTokenSets.FLAGS.types
        .map { it to BazelQueryHighlightingColors.FLAG }
        .toTypedArray(),
      *BazelQueryTokenSets.FLAG_VALS.types
        .map { it to BazelQueryHighlightingColors.FLAG_VAL }
        .toTypedArray(),
    )

  override fun getHighlightingLexer(): Lexer = BazelQueryLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(keys[tokenType])
}

class BazelQuerySyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter = BazelQuerySyntaxHighlighter
}
