package org.jetbrains.bazel.languages.bazelrc.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes
import org.jetbrains.bazel.languages.bazelrc.lexer.BazelrcHighlightingLexer

object BazelrcSyntaxHighlighter : SyntaxHighlighterBase() {
  private val keys =
    mapOf(
      BazelrcTokenTypes.COMMENT to BazelrcHighlightingColors.COMMENT,
      BazelrcTokenTypes.COMMAND to BazelrcHighlightingColors.KEYWORD,
      BazelrcTokenTypes.CONFIG to BazelrcHighlightingColors.IDENTIFIER,
      BazelrcTokenTypes.FLAG to BazelrcHighlightingColors.STRING,
    )

  override fun getHighlightingLexer(): Lexer = BazelrcHighlightingLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(keys[tokenType])
}

class BazelrcSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter = BazelrcSyntaxHighlighter
}
