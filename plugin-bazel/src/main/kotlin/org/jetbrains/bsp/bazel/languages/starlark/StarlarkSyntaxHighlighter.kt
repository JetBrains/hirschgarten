package org.jetbrains.bsp.bazel.languages.starlark

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType

object StarlarkSyntaxHighlighter : SyntaxHighlighterBase() {

    private val keys: Map<IElementType, TextAttributesKey> = mapOf(
        StarlarkTypes.DEF to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.LAMBDA to StarlarkHighlightingColors.KEYWORD,

        StarlarkTypes.IF to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.ELIF to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.ELSE to StarlarkHighlightingColors.KEYWORD,

        StarlarkTypes.FOR to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.IN to StarlarkHighlightingColors.KEYWORD,

        StarlarkTypes.RETURN to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.BREAK to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.CONTINUE to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.PASS to StarlarkHighlightingColors.KEYWORD,

        StarlarkTypes.LOAD to StarlarkHighlightingColors.KEYWORD,

        // boolean operators
        StarlarkTypes.OR to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.AND to StarlarkHighlightingColors.KEYWORD,
        StarlarkTypes.NOT to StarlarkHighlightingColors.KEYWORD,

        // code organization
        StarlarkTypes.SEMICOLON to StarlarkHighlightingColors.SEMICOLON,
        StarlarkTypes.COMMA to StarlarkHighlightingColors.COMMA,
        StarlarkTypes.DOT to StarlarkHighlightingColors.DOT,

        StarlarkTypes.LEFT_PAREN to StarlarkHighlightingColors.PARENTHESES,
        StarlarkTypes.RIGHT_PAREN to StarlarkHighlightingColors.PARENTHESES,

        StarlarkTypes.LEFT_CURLY to StarlarkHighlightingColors.BRACKETS,
        StarlarkTypes.RIGHT_CURLY to StarlarkHighlightingColors.BRACKETS,

        // random
        StarlarkTypes.INT to StarlarkHighlightingColors.NUMBER,
        StarlarkTypes.FLOAT to StarlarkHighlightingColors.NUMBER,

        StarlarkTypes.IDENTIFIER to StarlarkHighlightingColors.IDENTIFIER,

        StarlarkTypes.STRING to StarlarkHighlightingColors.STRING,
        StarlarkTypes.BYTES to StarlarkHighlightingColors.STRING,

        StarlarkTypes.COMMENT to StarlarkHighlightingColors.LINE_COMMENT,
    )

    override fun getHighlightingLexer(): Lexer = StarlarkLexerAdapter

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(keys[tokenType])
}

class StarlarkSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        StarlarkSyntaxHighlighter
}
