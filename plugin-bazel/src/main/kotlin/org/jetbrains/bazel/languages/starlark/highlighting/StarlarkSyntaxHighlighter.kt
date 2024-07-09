package org.jetbrains.bazel.languages.starlark.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.lexer.StarlarkHighlightingLexer

object StarlarkSyntaxHighlighter : SyntaxHighlighterBase() {
  private val keys = mapOf(
    StarlarkTokenTypes.DEF_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.LAMBDA_KEYWORD to StarlarkHighlightingColors.KEYWORD,

    StarlarkTokenTypes.IF_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.ELIF_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.ELSE_KEYWORD to StarlarkHighlightingColors.KEYWORD,

    StarlarkTokenTypes.FOR_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.IN_KEYWORD to StarlarkHighlightingColors.KEYWORD,

    StarlarkTokenTypes.RETURN_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.BREAK_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.CONTINUE_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.PASS_KEYWORD to StarlarkHighlightingColors.KEYWORD,

    StarlarkTokenTypes.LOAD_KEYWORD to StarlarkHighlightingColors.KEYWORD,

    // boolean operators
    StarlarkTokenTypes.OR_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.AND_KEYWORD to StarlarkHighlightingColors.KEYWORD,
    StarlarkTokenTypes.NOT_KEYWORD to StarlarkHighlightingColors.KEYWORD,

    // code organization
    StarlarkTokenTypes.SEMICOLON to StarlarkHighlightingColors.SEMICOLON,
    StarlarkTokenTypes.COMMA to StarlarkHighlightingColors.COMMA,
    StarlarkTokenTypes.DOT to StarlarkHighlightingColors.DOT,

    StarlarkTokenTypes.LPAR to StarlarkHighlightingColors.PARENTHESES,
    StarlarkTokenTypes.RPAR to StarlarkHighlightingColors.PARENTHESES,

    StarlarkTokenTypes.LBRACE to StarlarkHighlightingColors.BRACKETS,
    StarlarkTokenTypes.RBRACE to StarlarkHighlightingColors.BRACKETS,

    // random
    StarlarkTokenTypes.INT to StarlarkHighlightingColors.NUMBER,
    StarlarkTokenTypes.FLOAT to StarlarkHighlightingColors.NUMBER,

    StarlarkTokenTypes.IDENTIFIER to StarlarkHighlightingColors.IDENTIFIER,

    StarlarkTokenTypes.STRING to StarlarkHighlightingColors.STRING,
    StarlarkTokenTypes.BYTES to StarlarkHighlightingColors.STRING,

    StarlarkTokenTypes.COMMENT to StarlarkHighlightingColors.LINE_COMMENT,
  )

  override fun getHighlightingLexer(): Lexer = StarlarkHighlightingLexer()

  override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(keys[tokenType])
}

class StarlarkSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
    StarlarkSyntaxHighlighter
}
